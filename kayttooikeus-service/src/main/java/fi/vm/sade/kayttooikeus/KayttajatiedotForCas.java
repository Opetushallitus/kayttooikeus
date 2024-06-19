package fi.vm.sade.kayttooikeus;

// Only used by SAMLAUthenticationHandler, removed later when it reads the username on top level obejct
@Deprecated
public record KayttajatiedotForCas(String username) {}
