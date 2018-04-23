package fi.vm.sade.kayttooikeus.service.exception;

public class PalvelukayttajaLoginException extends RuntimeException {

    public PalvelukayttajaLoginException(String message) {
        super(message);
    }

    public PalvelukayttajaLoginException(String message, Throwable cause) {
        super(message, cause);
    }

    public PalvelukayttajaLoginException(Throwable cause) {
        super(cause);
    }

}
