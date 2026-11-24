package jahjah.shared.exceptions;

/** خطأ في القراءة أو الكتابة (يرميه DAL). */
public class DataAccessException extends RuntimeException {
    public DataAccessException(String message) { super(message); }

    public DataAccessException(String message, Throwable cause) { super(message, cause); }
}
