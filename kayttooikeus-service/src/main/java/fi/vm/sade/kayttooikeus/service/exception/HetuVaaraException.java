package fi.vm.sade.kayttooikeus.service.exception;

public class HetuVaaraException extends RuntimeException {

    public HetuVaaraException(String message) {
        super(message);
    }

    public HetuVaaraException(String message, Throwable cause) {
        super(message, cause);
    }

    public HetuVaaraException(Throwable cause) {
        super(cause);
    }

}
