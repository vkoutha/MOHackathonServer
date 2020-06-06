import java.io.Serializable;

public class Student implements Serializable {

    private String fullName;
    private String username;
    private String password;
    private String teacherName;

    public Student(String fullName, String username, String password, String teacherName){
        this.fullName = fullName;
        this.username = username;
        this.password = password;
        this.teacherName = teacherName;
    }

    public String getFullName() {
        return fullName;
    }

    public String getTeacherName() {
        return teacherName;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

}
