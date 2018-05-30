package fi.vm.sade.kayttooikeus.service.external.impl;

import fi.vm.sade.kayttooikeus.service.external.OrganisaatioPerustieto;

import java.util.*;
import java.util.stream.Stream;

import static java.util.Optional.ofNullable;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

public class OrganisaatioCache {
    private final OrganisaatioPerustieto root;
    private final Map<String,OrganisaatioPerustieto> byOid;

    public OrganisaatioCache(OrganisaatioPerustieto root, List<OrganisaatioPerustieto> rootChildren) {
        this.root = root;
        this.byOid = new HashMap<>();
        root.setChildren(rootChildren);
        this.setParents(root, rootChildren);
        this.byOid.put(root.getOid(), root);
        this.byOid.putAll(rootChildren.stream().flatMap(OrganisaatioPerustieto::andChildren)
            .collect(toMap(OrganisaatioPerustieto::getOid, identity())));
    }
    
    private void setParents(OrganisaatioPerustieto root, Collection<OrganisaatioPerustieto> children) {
        children.forEach(c -> {
            c.setParent(root);
            this.setParents(c, c.getChildren());
        });
    }
    
    public OrganisaatioPerustieto getRoot() {
        return root;
    }
    
    public Optional<OrganisaatioPerustieto> getByOid(String oid) {
        return ofNullable(byOid.get(oid));
    }
    
    public Stream<OrganisaatioPerustieto> flatWithChildrenByOid(String oid) {
        return getByOid(oid).map(OrganisaatioPerustieto::andChildren).orElse(Stream.empty());
    }

    public Stream<OrganisaatioPerustieto> flatWithParentsByOid(String oid) {
        return getByOid(oid).map(OrganisaatioPerustieto::andParents).orElse(Stream.empty());
    }

    public Stream<OrganisaatioPerustieto> flatWithParentsAndChildren(String oid) {
        return getByOid(oid).map(org -> Stream.concat(org.parents(), org.andChildren())).orElse(Stream.empty());
    }

    public Stream<OrganisaatioPerustieto> getAllOrganisaatios() {
        return this.byOid.values().stream();
    }

    public Long getCacheCount() {
        return (long) this.byOid.size();
    }
}
