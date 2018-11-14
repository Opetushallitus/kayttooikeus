package fi.vm.sade.kayttooikeus.service.exception;

public class LoginTokenException extends RuntimeException {

    public LoginTokenException() {
    }

    public LoginTokenException(String message) {
        super(message);
    }

    public LoginTokenException(String message, Throwable cause) {
        super(message, cause);
    }

    public LoginTokenException(Throwable cause) {
        super(cause);
    }


}
