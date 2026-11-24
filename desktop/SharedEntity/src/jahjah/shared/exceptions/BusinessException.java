package jahjah.shared.exceptions;

/** خطأ في قاعدة منطقية (يرميه BAL). */
public class BusinessException extends RuntimeException {
    public BusinessException(String message) { super(message); }
}
