import * as path from "node:path";

import * as aws_sns from "aws-cdk-lib/aws-sns";
import * as cdk from "aws-cdk-lib";
import * as certificatemanager from "aws-cdk-lib/aws-certificatemanager";
import * as constructs from "constructs";
import * as ec2 from "aws-cdk-lib/aws-ec2";
import * as ecr_assets from "aws-cdk-lib/aws-ecr-assets";
import * as ecs from "aws-cdk-lib/aws-ecs";
import * as elasticloadbalancingv2 from "aws-cdk-lib/aws-elasticloadbalancingv2";
import * as logs from "aws-cdk-lib/aws-logs";
import * as rds from "aws-cdk-lib/aws-rds";
import * as route53 from "aws-cdk-lib/aws-route53";
import * as route53_targets from "aws-cdk-lib/aws-route53-targets";
import * as ssm from "aws-cdk-lib/aws-ssm";

import { ALARM_TOPIC_ARN, prefix, QUALIFIER, VPC_NAME } from "./shared-account";
import { getConfig, getEnvironment } from "./config";

class CdkApp extends cdk.App {
  constructor(props: cdk.AppProps) {
    super(props);
    const stackProps = {
      env: {
        account: process.env.CDK_DEPLOY_TARGET_ACCOUNT,
        region: process.env.CDK_DEPLOY_TARGET_REGION,
      },
    };

    new DnsStack(this, prefix("DnsStack"), stackProps);
    new AlarmStack(this, prefix("AlarmStack"), stackProps);
    new ApplicationStack(this, prefix("ApplicationStack"), stackProps);
  }
}

class DnsStack extends cdk.Stack {
  constructor(scope: constructs.Construct, id: string, props: cdk.StackProps) {
    super(scope, id, props);

    new route53.HostedZone(this, "OtuvaHostedZone", {
      zoneName: ssm.StringParameter.valueFromLookup(this, "/otuva/zoneName"),
    });
  }
}

class AlarmStack extends cdk.Stack {
  constructor(scope: constructs.Construct, id: string, props: cdk.StackProps) {
    super(scope, id, props);
    aws_sns.Topic.fromTopicArn(this, "AlarmTopic", ALARM_TOPIC_ARN);
  }
}

class ApplicationStack extends cdk.Stack {
  constructor(scope: constructs.Construct, id: string, props: cdk.StackProps) {
    super(scope, id, props);
    const config = getConfig();
    const vpc = ec2.Vpc.fromLookup(this, "Vpc", { vpcName: VPC_NAME });

    const dbSecurityGroup = new ec2.SecurityGroup(
      this,
      "DatabaseSecurityGroup",
      { vpc },
    );

    const database = new rds.DatabaseCluster(this, "Database", {
      vpc,
      vpcSubnets: { subnetType: ec2.SubnetType.PRIVATE_ISOLATED },
      securityGroups: [dbSecurityGroup],
      defaultDatabaseName: "kayttooikeus",
      engine: rds.DatabaseClusterEngine.auroraPostgres({
        version: rds.AuroraPostgresEngineVersion.VER_12_17,
      }),
      credentials: rds.Credentials.fromGeneratedSecret("kayttooikeus"),
      storageType: rds.DBClusterStorageType.AURORA,
      writer: rds.ClusterInstance.provisioned("writer", {
        instanceType: ec2.InstanceType.of(
          ec2.InstanceClass.T4G,
          ec2.InstanceSize.LARGE,
        ),
      }),
      readers: [],
    });

    const cluster = new ecs.Cluster(this, "Cluster", {
      vpc,
      clusterName: "kayttooikeus",
    });

    const logGroup = new logs.LogGroup(this, "AppLogGroup", {
      logGroupName: "kayttooikeus",
      retention: logs.RetentionDays.INFINITE,
    });

    const dockerImage = new ecr_assets.DockerImageAsset(this, "AppImage", {
      directory: path.join(__dirname, "../../"),
      file: "Dockerfile",
      exclude: ["infra/"],
      platform: ecr_assets.Platform.LINUX_ARM64,
    });

    const taskDefinition = new ecs.FargateTaskDefinition(
      this,
      "TaskDefinition",
      {
        cpu: 1024,
        memoryLimitMiB: 2048,
        runtimePlatform: {
          operatingSystemFamily: ecs.OperatingSystemFamily.LINUX,
          cpuArchitecture: ecs.CpuArchitecture.ARM64,
        },
      },
    );
    const appPort = 8080;
    taskDefinition.addContainer("AppContainer", {
      image: ecs.ContainerImage.fromDockerImageAsset(dockerImage),
      logging: new ecs.AwsLogDriver({ logGroup, streamPrefix: "app" }),
      environment: {
        ENV: getEnvironment() === "hahtuva" ? "hahtuva2" : getEnvironment(),
        postgres_host: database.clusterEndpoint.hostname,
        postgres_port: database.clusterEndpoint.port.toString(),
        postgres_database: "kayttooikeus",
      },
      secrets: {
        postgres_username: ecs.Secret.fromSecretsManager(
          database.secret!,
          "username",
        ),
        postgres_password: ecs.Secret.fromSecretsManager(
          database.secret!,
          "password",
        ),
        ssm_lampi_role_arn: this.ssmSecret("LampiRoleArn"),
        ssm_lampi_external_id: this.ssmSecret("LampiExternalId"),
        ssm_cas_mfa_username: this.ssmSecret("CasMfaUsername"),
        ssm_cas_mfa_password: this.ssmSecret("CasMfaPassword"),
        ssm_cas_gauth_encryption_key: this.ssmSecret("CasGauthEncryptionKey"),
        ssm_cas_gauth_signing_key: this.ssmSecret("CasGauthSigningKey"),
        ssm_kayttooikeus_username: this.ssmSecret("PalvelukayttajaUsername"),
        ssm_kayttooikeus_password: this.ssmSecret("PalvelukayttajaPassword"),
        ssm_kayttooikeus_crypto_password: this.ssmSecret("CryptoPassword"),
        ssm_kayttooikeus_kutsu_allowlist: this.ssmSecret("KutsuAllowlist"),
        ssm_auth_cryptoservice_static_salt: this.ssmSecret(
          "CryptoserviceStaticSalt",
        ),
      },
      portMappings: [
        {
          name: "kayttooikeus",
          containerPort: appPort,
          hostPort: appPort,
          appProtocol: ecs.AppProtocol.http,
        },
      ],
    });

    const appSecurityGroup = new ec2.SecurityGroup(this, "AppSecurityGroup", {
      vpc,
    });

    const service = new ecs.FargateService(this, "Service", {
      cluster,
      taskDefinition,
      desiredCount: config.minCapacity,
      minHealthyPercent: 100,
      maxHealthyPercent: 200,
      vpcSubnets: { subnetType: ec2.SubnetType.PRIVATE_WITH_EGRESS },
      securityGroups: [appSecurityGroup],
      healthCheckGracePeriod: cdk.Duration.minutes(5),
    });
    const scaling = service.autoScaleTaskCount({
      minCapacity: config.minCapacity,
      maxCapacity: config.maxCapacity,
    });

    // Scaling roughly the same as DownScalingPolicy and
    scaling.scaleOnMetric("ServiceScaling", {
      metric: service.metricCpuUtilization(),
      scalingSteps: [
        { upper: 15, change: -1 },
        { lower: 50, change: +1 },
        { lower: 65, change: +2 },
        { lower: 80, change: +3 },
      ],
    });

    dbSecurityGroup.addIngressRule(
      appSecurityGroup,
      ec2.Port.tcp(database.clusterEndpoint.port),
    );

    const albSecurityGroup = new ec2.SecurityGroup(this, "AlbSecurityGroup", {
      vpc,
    });
    const alb = new elasticloadbalancingv2.ApplicationLoadBalancer(
      this,
      "LoadBalancer",
      {
        vpc,
        internetFacing: true,
        securityGroup: albSecurityGroup,
      },
    );

    const sharedHostedZone = route53.HostedZone.fromLookup(
      this,
      "YleiskayttoisetHostedZone",
      {
        domainName: ssm.StringParameter.valueFromLookup(this, "zoneName"),
      },
    );
    const albHostname = `kayttooikeus.${sharedHostedZone.zoneName}`;
    const albRecord = new route53.ARecord(this, "ALBARecord", {
      zone: sharedHostedZone,
      recordName: albHostname,
      target: route53.RecordTarget.fromAlias(
        new route53_targets.LoadBalancerTarget(alb),
      ),
    });

    const albCertificate = new certificatemanager.Certificate(
      this,
      "AlbCertificate",
      {
        domainName: albHostname,
        validation:
          certificatemanager.CertificateValidation.fromDns(sharedHostedZone),
      },
    );

    const listener = alb.addListener("Listener", {
      protocol: elasticloadbalancingv2.ApplicationProtocol.HTTPS,
      port: 443,
      open: true,
      certificates: [albCertificate],
    });

    listener.addTargets("ServiceTarget", {
      port: appPort,
      targets: [service],
      healthCheck: {
        enabled: true,
        interval: cdk.Duration.seconds(10),
        path: "/kayttooikeus-service/actuator/health",
        port: appPort.toString(),
      },
    });
  }

  ssmSecret(name: string): ecs.Secret {
    return ecs.Secret.fromSsmParameter(
      ssm.StringParameter.fromSecureStringParameterAttributes(
        this,
        `Param${name}`,
        { parameterName: `/kayttooikeus/${name}` },
      ),
    );
  }
}

const app = new CdkApp({
  defaultStackSynthesizer: new cdk.DefaultStackSynthesizer({
    qualifier: QUALIFIER,
  }),
});
app.synth();