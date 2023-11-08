class ErrorDto {

    private final int code;

    private java.lang.String message = null;

    public ErrorDto(final int code) {
        this.code = code;
    }

    public int getCode() {
        return this.code;
    }

    public java.lang.String getMessage() {
        return this.message;
    }

    public void setMessage(final java.lang.String message) {
        this.message = message;
    }

    public ErrorDto message(final java.lang.String message) {
        this.message = message;
        return this;
    }
}
