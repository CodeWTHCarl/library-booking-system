package model;

/** A library patron. Demonstrates ENCAPSULATION: private fields, validated setters, overloaded constructors. */
public class Client {
    private int clientId;
    private String fullName;
    private String studentId;
    private String contactNo;

    /** For a NEW client that is not in the database yet (id is assigned by MySQL). */
    public Client(String fullName, String studentId, String contactNo) {
        this(0, fullName, studentId, contactNo);
    }

    /** For a client loaded FROM the database. */
    public Client(int clientId, String fullName, String studentId, String contactNo) {
        this.clientId = clientId;
        setFullName(fullName);
        setStudentId(studentId);
        setContactNo(contactNo);
    }

    public int getClientId() {
        return clientId;
    }

    public void setClientId(int clientId) {
        this.clientId = clientId;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = requireText(fullName, "Full name");
    }

    public String getStudentId() {
        return studentId;
    }

    public void setStudentId(String studentId) {
        this.studentId = requireText(studentId, "Student ID");
    }

    public String getContactNo() {
        return contactNo;
    }

    public void setContactNo(String contactNo) {
        this.contactNo = requireText(contactNo, "Contact number");
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + " must not be empty.");
        }
        return value.trim();
    }

    @Override
    public String toString() {
        return fullName + " (" + studentId + ")";
    }
}
