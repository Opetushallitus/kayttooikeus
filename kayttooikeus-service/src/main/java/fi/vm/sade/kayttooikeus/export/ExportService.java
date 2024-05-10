package fi.vm.sade.kayttooikeus.export;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.transfer.s3.S3TransferManager;
import software.amazon.awssdk.transfer.s3.model.DownloadFileRequest;
import software.amazon.awssdk.transfer.s3.model.UploadFileRequest;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExportService {
    private static final String S3_PREFIX = "fulldump/kayttooikeus/v2";
    private final ObjectMapper objectMapper = new ObjectMapper()
            .setSerializationInclusion(JsonInclude.Include.NON_NULL);
    private final JdbcTemplate jdbcTemplate;
    private final S3AsyncClient opintopolkuS3Client;
    private final S3AsyncClient lampiS3Client;

    @Value("${kayttooikeus.tasks.export.bucket-name}")
    private String bucketName;
    @Value("${kayttooikeus.tasks.export.lampi-bucket-name}")
    private String lampiBucketName;
    @Value("${kayttooikeus.tasks.export.upload-to-s3:true}")
    private boolean uploadToS3;

    @Transactional
    public void createSchema() {
        jdbcTemplate.execute("DROP SCHEMA IF EXISTS exportnew CASCADE");
        jdbcTemplate.execute("CREATE SCHEMA exportnew");
        jdbcTemplate.execute("""
                CREATE TABLE exportnew.kayttooikeusryhma_kayttooikeus AS
                SELECT
                    kayttooikeusryhma_id,
                    kayttooikeus_id
                FROM kayttooikeusryhma_kayttooikeus
                """);
        jdbcTemplate.execute("""
                CREATE TABLE exportnew.kayttooikeus AS
                SELECT
                    k.id AS id,
                    tp.text AS palvelu,
                    tk.text AS kayttooikeus,
                    k.rooli AS rooli
                FROM kayttooikeus k
                LEFT JOIN text_group tgk ON k.textgroup_id = tgk.id
                LEFT JOIN text tk ON tk.textgroup_id = tgk.id AND tk.lang = 'FI'
                LEFT JOIN palvelu p ON k.palvelu_id = p.id
                LEFT JOIN text_group tgp ON p.textgroup_id = tgp.id
                LEFT JOIN text tp ON tp.textgroup_id = tgp.id AND tp.lang = 'FI'
                """);
        jdbcTemplate.execute("""
                CREATE TABLE exportnew.kayttooikeusryhma AS
                SELECT
                    k.id AS id,
                    tk.text AS nimi,
                    tp.text AS kuvaus,
                    k.allowed_usertype AS kayttajatyyppi
                FROM kayttooikeusryhma k
                LEFT JOIN text_group tgk ON k.textgroup_id = tgk.id
                LEFT JOIN text tk ON tk.textgroup_id = tgk.id AND tk.lang = 'FI'
                LEFT JOIN text_group tgp ON k.kuvaus_id = tgp.id
                LEFT JOIN text tp ON tp.textgroup_id = tgp.id AND tp.lang = 'FI'
                """);
        jdbcTemplate.execute("""
                CREATE TABLE exportnew.henkilo_kayttooikeusryhma AS
                SELECT
                    h.oidhenkilo AS henkilo_oid,
                    oh.organisaatio_oid AS organisaatio_oid,
                    mkt.kayttooikeusryhma_id AS kayttooikeusryhma_id,
                    mkt.voimassaalkupvm AS voimassaalkupvm,
                    mkt.voimassaloppupvm AS voimassaloppupvm
                FROM myonnetty_kayttooikeusryhma_tapahtuma mkt
                LEFT JOIN organisaatiohenkilo oh ON mkt.organisaatiohenkilo_id = oh.id
                LEFT JOIN henkilo h ON h.id = oh.henkilo_id
                """);
        jdbcTemplate.execute("""
                CREATE TABLE exportnew.henkilo_kayttooikeusryhma_historia AS
                SELECT
                    h.oidhenkilo AS henkilo_oid,
                    oh.organisaatio_oid AS organisaatio_oid,
                    kth.kayttooikeusryhma_id AS kayttooikeusryhma_id,
                    kth.aikaleima AS arkistointiaika
                FROM kayttooikeusryhma_tapahtuma_historia kth
                LEFT JOIN organisaatiohenkilo oh ON kth.organisaatiohenkilo_id = oh.id
                LEFT JOIN henkilo h ON h.id = oh.henkilo_id
                """);

        jdbcTemplate.execute("ALTER TABLE exportnew.kayttooikeus ADD CONSTRAINT kayttooikeus_pk PRIMARY KEY (id)");
        jdbcTemplate.execute("ALTER TABLE exportnew.kayttooikeusryhma ADD CONSTRAINT kayttooikeusryhma_pk PRIMARY KEY (id)");
        jdbcTemplate.execute("ALTER TABLE exportnew.kayttooikeusryhma_kayttooikeus ADD CONSTRAINT kayttooikeusryhma_kayttooikeus_pk PRIMARY KEY (kayttooikeusryhma_id, kayttooikeus_id)");
        jdbcTemplate.execute("ALTER TABLE exportnew.kayttooikeusryhma_kayttooikeus ADD CONSTRAINT kayttooikeusryhma_kayttooikeus_kayttooikeusryhma_fk FOREIGN KEY (kayttooikeusryhma_id) REFERENCES kayttooikeusryhma (id)");
        jdbcTemplate.execute("ALTER TABLE exportnew.kayttooikeusryhma_kayttooikeus ADD CONSTRAINT kayttooikeusryhma_kayttooikeus_kayttooikeus_fk FOREIGN KEY (kayttooikeus_id) REFERENCES kayttooikeus(id)");
        jdbcTemplate.execute("ALTER TABLE exportnew.henkilo_kayttooikeusryhma ADD CONSTRAINT henkilo_kayttooikeusryhma_kayttooikeusryhma_fk FOREIGN KEY (kayttooikeusryhma_id) REFERENCES kayttooikeusryhma(id)");
        jdbcTemplate.execute("ALTER TABLE exportnew.henkilo_kayttooikeusryhma_historia ADD CONSTRAINT henkilo_kayttooikeusryhma_historia_kayttooikeusryhma_fk FOREIGN KEY (kayttooikeusryhma_id) REFERENCES kayttooikeusryhma(id)");

        jdbcTemplate.execute("DROP SCHEMA IF EXISTS export CASCADE");
        jdbcTemplate.execute("ALTER SCHEMA exportnew RENAME TO export");
    }

    private static final String KAYTTOOIKEUS_QUERY = "SELECT id, palvelu, kayttooikeus, rooli FROM export.kayttooikeus";
    private static final String KAYTTOOIKEUSRYHMA_QUERY = "SELECT id, nimi, kuvaus, kayttajatyyppi FROM export.kayttooikeusryhma";
    private static final String KAYTTOOIKEUSRYHMA_RELAATIO_QUERY = "SELECT kayttooikeusryhma_id, kayttooikeus_id FROM export.kayttooikeusryhma_kayttooikeus";
    private static final String HENKILO_KAYTTOOIKEUS_QUERY = "SELECT henkilo_oid, organisaatio_oid, kayttooikeusryhma_id, voimassaalkupvm, voimassaloppupvm FROM export.henkilo_kayttooikeusryhma";
    private static final String HENKILO_KAYTTOOIKEUS_HISTORIA_QUERY = "SELECT henkilo_oid, organisaatio_oid, kayttooikeusryhma_id, arkistointiaika FROM export.henkilo_kayttooikeusryhma_historia";

    public void generateExportFiles() throws IOException {
        generateCsvExports();
        generateJsonExports();
    }

    void generateCsvExports() {
        exportQueryToS3(S3_PREFIX + "/csv/kayttooikeus.csv", KAYTTOOIKEUS_QUERY);
        exportQueryToS3(S3_PREFIX + "/csv/kayttooikeusryhma.csv", KAYTTOOIKEUSRYHMA_QUERY);
        exportQueryToS3(S3_PREFIX + "/csv/kayttooikeusryhma_kayttooikeus.csv", KAYTTOOIKEUSRYHMA_RELAATIO_QUERY);
        exportQueryToS3(S3_PREFIX + "/csv/henkilo_kayttooikeusryhma.csv", HENKILO_KAYTTOOIKEUS_QUERY);
        exportQueryToS3(S3_PREFIX + "/csv/henkilo_kayttooikeusryhma_historia.csv", HENKILO_KAYTTOOIKEUS_HISTORIA_QUERY);
    }

    List<File> generateJsonExports() throws IOException {
        var kayttooikeusFile = exportQueryToS3AsJson(KAYTTOOIKEUS_QUERY, S3_PREFIX + "/json/kayttooikeus.json", unchecked(rs ->
                new Kayttooikeus(
                        rs.getLong("id"),
                        rs.getString("palvelu"),
                        rs.getString("kayttooikeus"),
                        rs.getString("rooli")
                )
        ));
        var kayttooikeusryhmaFile = exportQueryToS3AsJson(KAYTTOOIKEUSRYHMA_QUERY, S3_PREFIX + "/json/kayttooikeusryhma.json", unchecked(rs ->
                new Kayttooikeusryhma(
                        rs.getLong("id"),
                        rs.getString("nimi"),
                        rs.getString("kuvaus"),
                        rs.getString("kayttajatyyppi")
                )
        ));
        var kayttooikeusryhmaRelaatioFile = exportQueryToS3AsJson(KAYTTOOIKEUSRYHMA_RELAATIO_QUERY, S3_PREFIX + "/json/kayttooikeusryhma_kayttooikeus.json", unchecked(rs ->
                new KayttooikeusryhmaRelaatio(
                        rs.getLong("kayttooikeusryhma_id"),
                        rs.getLong("kayttooikeus_id")
                )
        ));
        var henkiloKayttooikeusryhmaFile = exportQueryToS3AsJson(HENKILO_KAYTTOOIKEUS_QUERY, S3_PREFIX + "/json/henkilo_kayttooikeusryhma.json", unchecked(rs ->
                new HenkiloKayttooikeusryhma(
                        rs.getString("henkilo_oid"),
                        rs.getString("organisaatio_oid"),
                        rs.getLong("kayttooikeusryhma_id"),
                        rs.getString("voimassaalkupvm"),
                        rs.getString("voimassaloppupvm")
                )
        ));
        var henkiloKayttooikeusryhmaHistoriaFile = exportQueryToS3AsJson(HENKILO_KAYTTOOIKEUS_HISTORIA_QUERY, S3_PREFIX + "/json/henkilo_kayttooikeusryhma_historia.json", unchecked(rs ->
                new HenkiloKayttooikeusryhmaHistoria(
                    rs.getString("henkilo_oid"),
                    rs.getString("organisaatio_oid"),
                    rs.getLong("kayttooikeusryhma_id"),
                    rs.getString("arkistointiaika")
                )
        ));
        return List.of(
            kayttooikeusFile,
            kayttooikeusryhmaFile,
            kayttooikeusryhmaRelaatioFile,
            henkiloKayttooikeusryhmaFile,
            henkiloKayttooikeusryhmaHistoriaFile
        );
    }

    private interface ThrowingFunction<T, R, E extends Throwable> {
        R apply(T rs) throws E;
    }


    private <T> File exportQueryToS3AsJson(String query, String objectKey, Function<ResultSet, T> mapper) throws IOException {
        var tempFile = File.createTempFile("export", ".json");
        try {
            exportToFile(query, mapper, tempFile);
            uploadFile(opintopolkuS3Client, bucketName, objectKey, tempFile);
        } finally {
            if (uploadToS3) {
                Files.deleteIfExists(tempFile.toPath());
            } else {
                log.info("Not uploading file to S3, keeping it at {}", tempFile.getAbsolutePath());
            }
        }
        return tempFile;
    }

    private <T> void exportToFile(String query, Function<ResultSet, T> mapper, File file) throws IOException {
        log.info("Writing JSON export to {}", file.getAbsolutePath());
        try (var writer = Files.newBufferedWriter(file.toPath(), StandardCharsets.UTF_8)) {
            writer.write("[\n");
            var firstElement = true;
            try (Stream<T> stream = jdbcTemplate.queryForStream(query, (rs, n) -> mapper.apply(rs))) {
                Iterable<T> iterable = stream::iterator;
                for (T jsonObject : iterable) {
                    if (firstElement) {
                        firstElement = false;
                    } else {
                        writer.write(",\n");
                    }
                    writer.write(objectMapper.writeValueAsString(jsonObject));
                }
            }
            writer.write("\n");
            writer.write("]\n");
        }
    }

    private <T, R, E extends Throwable> Function<T, R> unchecked(ThrowingFunction<T, R, E> f) {
        return t -> {
            try {
                return f.apply(t);
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
        };
    }

    private void exportQueryToS3(String objectKey, String query) {
        log.info("Exporting table to S3: {}/{}", bucketName, objectKey);
        var sql = "SELECT rows_uploaded FROM aws_s3.query_export_to_s3(?, aws_commons.create_s3_uri(?, ?, ?), options := 'FORMAT CSV, HEADER TRUE')";
        var rowsUploaded = jdbcTemplate.queryForObject(sql, Long.class, query, bucketName, objectKey, OpintopolkuAwsClients.REGION.id());
        log.info("Exported {} rows to S3 object {}", rowsUploaded, objectKey);
    }

    public void copyExportFilesToLampi() throws IOException {
        var csvManifest = new ArrayList<ExportManifest.ExportFileDetails>();
        csvManifest.add(copyFileToLampi(S3_PREFIX + "/csv/kayttooikeus.csv"));
        csvManifest.add(copyFileToLampi(S3_PREFIX + "/csv/kayttooikeusryhma.csv"));
        csvManifest.add(copyFileToLampi(S3_PREFIX + "/csv/kayttooikeusryhma_kayttooikeus.csv"));
        csvManifest.add(copyFileToLampi(S3_PREFIX + "/csv/henkilo_kayttooikeusryhma.csv"));
        csvManifest.add(copyFileToLampi(S3_PREFIX + "/csv/henkilo_kayttooikeusryhma_historia.csv"));
        writeManifest(S3_PREFIX + "/csv/manifest.json", new ExportManifest(csvManifest));

        var jsonManifest = new ArrayList<ExportManifest.ExportFileDetails>();
        jsonManifest.add(copyFileToLampi(S3_PREFIX + "/json/kayttooikeus.json"));
        jsonManifest.add(copyFileToLampi(S3_PREFIX + "/json/kayttooikeusryhma.json"));
        jsonManifest.add(copyFileToLampi(S3_PREFIX + "/json/kayttooikeusryhma_kayttooikeus.json"));
        jsonManifest.add(copyFileToLampi(S3_PREFIX + "/json/henkilo_kayttooikeusryhma.json"));
        jsonManifest.add(copyFileToLampi(S3_PREFIX + "/json/henkilo_kayttooikeusryhma_historia.json"));
        writeManifest(S3_PREFIX + "/json/manifest.json", new ExportManifest(jsonManifest));
    }

    private void writeManifest(String objectKey, ExportManifest manifest) throws JsonProcessingException {
        var manifestJson = objectMapper.writeValueAsString(manifest);
        var response = lampiS3Client.putObject(
                b -> b.bucket(lampiBucketName).key(objectKey),
                AsyncRequestBody.fromString(manifestJson)
        ).join();
        log.info("Wrote manifest to S3: {}", response);
    }

    private ExportManifest.ExportFileDetails copyFileToLampi(String objectKey) throws IOException {
        var temporaryFile = File.createTempFile("export", ".csv");
        try {
            log.info("Downloading file from S3: {}/{}", bucketName, objectKey);
            try (var downloader = S3TransferManager.builder().s3Client(opintopolkuS3Client).build()) {
                var fileDownload = downloader.downloadFile(DownloadFileRequest.builder()
                        .getObjectRequest(b -> b.bucket(bucketName).key(objectKey))
                        .destination(temporaryFile)
                        .build());
                fileDownload.completionFuture().join();
            }

            var response = uploadFile(lampiS3Client, lampiBucketName, objectKey, temporaryFile);
            return new ExportManifest.ExportFileDetails(objectKey, response.versionId());
        } finally {
            Files.deleteIfExists(temporaryFile.toPath());
        }
    }

    private PutObjectResponse uploadFile(S3AsyncClient s3Client, String bucketName, String objectKey, File file) {
        if (!uploadToS3) {
            log.info("Skipping upload to S3");
            return null;
        }
        log.info("Uploading file to S3: {}/{}", bucketName, objectKey);
        try (var uploader = S3TransferManager.builder().s3Client(s3Client).build()) {
            var fileUpload = uploader.uploadFile(UploadFileRequest.builder()
                    .putObjectRequest(b -> b.bucket(bucketName).key(objectKey))
                    .source(file)
                    .build());
            var result = fileUpload.completionFuture().join();
            return result.response();
        }
    }
}

record Kayttooikeus(Long id, String palvelu, String kayttooikeus, String rooli){}
record Kayttooikeusryhma(Long id, String palvelu, String kayttooikeus, String rooli){}
record KayttooikeusryhmaRelaatio(Long kayttooikeusryhmaId, Long kayttooikeusId){}
record HenkiloKayttooikeusryhma(String henkiloOid, String organisaatioOid, Long kayttooikeusryhmaId, String voimassaalkupvm, String voimassaloppupvm){}
record HenkiloKayttooikeusryhmaHistoria(String henkiloOid, String organisaatioOid, Long kayttooikeusryhmaId, String arkistointiaika){}