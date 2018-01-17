package fi.vm.sade.kayttooikeus.service.exception;

public class HetuKaytossaException extends RuntimeException {

    public HetuKaytossaException(String message) {
        super(message);
    }

    public HetuKaytossaException(String message, Throwable cause) {
        super(message, cause);
    }

    public HetuKaytossaException(Throwable cause) {
        super(cause);
    }

}
