package smarticulous;

import smarticulous.db.Exercise;
import smarticulous.db.Submission;
import smarticulous.db.User;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * The Smarticulous class, implementing a grading system.
 */
public class Smarticulous {

    /**
     * The connection to the underlying DB.
     * <p>
     * null if the db has not yet been opened.
     */
    Connection db;

    /**
     * Open the {@link Smarticulous} SQLite database.
     * <p>
     * This should open the database, creating a new one if necessary, and set the {@link #db} field
     * to the new connection.
     * <p>
     * The open method should make sure the database contains the following tables, creating them if necessary:
     *
     * <table>
     *   <caption><em>Table name: <strong>User</strong></em></caption>
     *   <tr><th>Column</th><th>Type</th></tr>
     *   <tr><td>UserId</td><td>Integer (Primary Key)</td></tr>
     *   <tr><td>Username</td><td>Text</td></tr>
     *   <tr><td>Firstname</td><td>Text</td></tr>
     *   <tr><td>Lastname</td><td>Text</td></tr>
     *   <tr><td>Password</td><td>Text</td></tr>
     * </table>
     *
     * <p>
     * <table>
     *   <caption><em>Table name: <strong>Exercise</strong></em></caption>
     *   <tr><th>Column</th><th>Type</th></tr>
     *   <tr><td>ExerciseId</td><td>Integer (Primary Key)</td></tr>
     *   <tr><td>Name</td><td>Text</td></tr>
     *   <tr><td>DueDate</td><td>Integer</td></tr>
     * </table>
     *
     * <p>
     * <table>
     *   <caption><em>Table name: <strong>Question</strong></em></caption>
     *   <tr><th>Column</th><th>Type</th></tr>
     *   <tr><td>ExerciseId</td><td>Integer</td></tr>
     *   <tr><td>QuestionId</td><td>Integer</td></tr>
     *   <tr><td>Name</td><td>Text</td></tr>
     *   <tr><td>Desc</td><td>Text</td></tr>
     *   <tr><td>Points</td><td>Integer</td></tr>
     * </table>
     * In this table the combination of ExerciseId and QuestionId together comprise the primary key.
     *
     * <p>
     * <table>
     *   <caption><em>Table name: <strong>Submission</strong></em></caption>
     *   <tr><th>Column</th><th>Type</th></tr>
     *   <tr><td>SubmissionId</td><td>Integer (Primary Key)</td></tr>
     *   <tr><td>UserId</td><td>Integer</td></tr>
     *   <tr><td>ExerciseId</td><td>Integer</td></tr>
     *   <tr><td>SubmissionTime</td><td>Integer</td></tr>
     * </table>
     *
     * <p>
     * <table>
     *   <caption><em>Table name: <strong>QuestionGrade</strong></em></caption>
     *   <tr><th>Column</th><th>Type</th></tr>
     *   <tr><td>SubmissionId</td><td>Integer</td></tr>
     *   <tr><td>QuestionId</td><td>Integer</td></tr>
     *   <tr><td>Grade</td><td>Real</td></tr>
     * </table>
     * In this table the combination of SubmissionId and QuestionId together comprise the primary key.
     *
     * @param dburl The JDBC url of the database to open (will be of the form "jdbc:sqlite:...")
     * @return the new connection
     * @throws SQLException
     */
    public Connection openDB(String dburl) throws SQLException {
        db = DriverManager.getConnection(dburl);
        Statement st = db.createStatement();
        //creating the User table
        st.execute("CREATE TABLE IF NOT EXISTS User (UserId INTEGER PRIMARY KEY, Username TEXT UINQUE, Firstname TEXT, Lastname TEXT, Password TEXT)");
        
        //creating the Exercize table
        st.execute("CREATE TABLE IF NOT EXISTS Exercise(ExerciseId INTEGER PRIMARY KEY, Name TEXT NOT NULL, DueDate INTEGER)");
        
        //creating the Question table
        st.execute("CREATE TABLE IF NOT EXISTS Question(ExerciseId INTEGER, QuestionId INTEGER, Name TEXT, Desc TEXT, Points INTEGER, PRIMARY KEY (ExerciseId, QuestionId))");
        
        //creating the Submission table
        st.execute("CREATE TABLE IF NOT EXISTS Submission(SubmissionId INTEGER PRIMARY KEY, UserId INTEGER, ExerciseId INTEGER, SubmissionTime INTEGER)");
        
        //creating the QuestionGrade table
        st.execute("CREATE TABLE IF NOT EXISTS QuestionGrade(SubmissionId INTEGER, QuestionId INTEGER, Grade REAL, PRIMARY KEY (SubmissionId, QuestionId))");
        
        return db;
    }


    /**
     * Close the DB if it is open.
     *
     * @throws SQLException
     */
    public void closeDB() throws SQLException {
        if (db != null) {
            db.close();
            db = null;
        }
    }

    // =========== User Management =============

    /**
     * Add a user to the database / modify an existing user.
     * <p>
     * Add the user to the database if they don't exist. If a user with user.username does exist,
     * update their password and firstname/lastname in the database.
     *
     * @param user
     * @param password
     * @return the userid.
     * @throws SQLException
     */
    public int addOrUpdateUser(User user, String password) throws SQLException {
        //updating or adding a user
        PreparedStatement st = db.prepareStatement("INSERT OR REPLACE INTO User (Username, Firstname, Lastname, Password) VALUES (?, ?, ?, ?)");
        st.setString(1, user.username);
        st.setString(2, user.firstname);
        st.setString(3, user.lastname);
        st.setString(4, password);
        st.executeUpdate();

        //getting the user's id
        PreparedStatement getId = db.prepareStatement("SELECT UserId FROM User WHERE Firstname = ? AND Lastname = ?");
        getId.setString(1, user.firstname);
        getId.setString(2, user.lastname);
        return getId.executeQuery().getInt("UserId");
    }


    /**
     * Verify a user's login credentials.
     *
     * @param username
     * @param password
     * @return true if the user exists in the database and the password matches; false otherwise.
     * @throws SQLException
     * <p>
     * Note: this is totally insecure. For real-life password checking, it's important to store only
     * a password hash
     * @see <a href="https://crackstation.net/hashing-security.htm">How to Hash Passwords Properly</a>
     */
    public boolean verifyLogin(String username, String password) throws SQLException {
        //using SELECT 1, if the row exits, @exits will get a value. else it won't
        PreparedStatement st = db.prepareStatement("SELECT 1 FROM User WHERE Username = ? AND Password = ?");
        st.setString(1, username);
        st.setString(2, password);
        ResultSet exits = st.executeQuery();
        return exits.next();
    }

    // =========== Exercise Management =============

    /**
     * Add an exercise to the database.
     *
     * @param exercise
     * @return the new exercise id, or -1 if an exercise with this id already existed in the database.
     * @throws SQLException
     */
    public int addExercise(Exercise exercise) throws SQLException {
        //if there exits such an exercise i'll get an SQLIntegrityConstraintViolationException and return -1, else return the id.
        try {
            //inserting to the Excercise table
            PreparedStatement st = db.prepareStatement("INSERT INTO Exercise (ExerciseId, Name, DueDate) VALUES (?, ?, ?)");
            st.setInt(1, exercise.id);
            st.setString(2, exercise.name);
            st.setLong(3, exercise.dueDate.getTime());
            
            //inserting into the Question table exercise's questions
            PreparedStatement quest = db.prepareStatement("INSERT INTO Question (ExerciseId, Name, Desc, Points) VALUES (?, ?, ?, ?)");
            for(int i=0; i<exercise.questions.size(); i++){
                quest.setInt(1, exercise.id);
                quest.setString(2, exercise.questions.get(i).name);
                quest.setString(3, exercise.questions.get(i).desc);
                quest.setInt(4, exercise.questions.get(i).points);
                quest.addBatch();
            }

            quest.executeBatch();
            st.executeUpdate();
            return exercise.id;
        } catch (SQLIntegrityConstraintViolationException e) {
            return -1;
        }
    }


    /**
     * Return a list of all the exercises in the database.
     * <p>
     * The list should be sorted by exercise id.
     *
     * @return list of all exercises.
     * @throws SQLException
     */
    public List<Exercise> loadExercises() throws SQLException {
        List<Exercise> exercises = new ArrayList<>();
        Statement st = db.createStatement();
        
        //retrieving the sorted exercises
        ResultSet rs = st.executeQuery("SELECT * FROM Exercise ORDER BY ExerciseId ASC");

        //creating the list
        while(rs.next()){
            exercises.add(new Exercise(rs.getInt(1), rs.getString(2), new Date(rs.getInt(3))));
            
            //deleting questions in the Question table from the current exercise. 
            PreparedStatement deleteQuestions = db.prepareStatement("DELETE FROM Question WHERE ExerciseId = ?");
            deleteQuestions.setInt(1, rs.getInt(1));
            deleteQuestions.executeUpdate();
        }

        return exercises;
    }

    // ========== Submission Storage ===============

    /**
     * Store a submission in the database.
     * The id field of the submission will be ignored if it is -1.
     * <p>
     * Return -1 if the corresponding user doesn't exist in the database.
     *
     * @param submission
     * @return the submission id.
     * @throws SQLException
     */
    public int storeSubmission(Submission submission) throws SQLException {
        
        //checking corresponding user exists in database.
        PreparedStatement test = db.prepareStatement("SELECT UserId FROM User WHERE Username = ?");
        test.setString(1, submission.user.username);
        ResultSet res = test.executeQuery();
        if (!res.next()) {
            return -1;
        }
       
        //storing submission details in Submission table.
        PreparedStatement st = db.prepareStatement("INSERT INTO Submission (SubmissionId, UserId, ExerciseId, SubmissionTime) VALUES (?, ?, ?, ?)");
        if(submission.id != -1){
            st.setInt(1, submission.id);
        }
        st.setInt(2, res.getInt(1));
        st.setInt(3, submission.exercise.id);
        st.setLong(4, submission.submissionTime.getTime());
        
        st.executeUpdate();
        //retrieving the ID
        PreparedStatement storedId = db.prepareStatement("SELECT SubmissionId FROM Submission WHERE UserId = ? AND SubmissionTime = ?");
        storedId.setInt(1, res.getInt(1));
        storedId.setLong(2, submission.submissionTime.getTime());
        return storedId.executeQuery().getInt(1);
    }


    // ============= Submission Query ===============


    /**
     * Return a prepared SQL statement that, when executed, will
     * return one row for every question of the latest submission for the given exer cise by the given user.
     * <p>
     * The rows should be sorted by QuestionId, and each row should contain:
     * - A column named "SubmissionId" with the submission id.
     * - A column named "QuestionId" with the question id,
     * - A column named "Grade" with the grade for that question.
     * - A column named "SubmissionTime" with the time of submission.
     * <p>
     * Parameter 1 of the prepared statement will be set to the User's username, Parameter 2 to the Exercise Id, and
     * Parameter 3 to the number of questions in the given exercise.
     * <p>
     * This will be used by {@link #getLastSubmission(User, Exercise)}
     *
     * @return
     */
    PreparedStatement getLastSubmissionGradesStatement() throws SQLException {
        String sql = "SELECT qg.SubmissionId, qg.QuestionId, qg.Grade, SubmissionTime"+
                     " FROM QuestionGrade as qg, " + 
                     " Submission as s ON s.SubmissionId = qg.SubmissionId,"+
                     " User ON User.UserId = s.UserId"+
                     " WHERE User.Username = ? AND s.ExerciseId = ? "+
                     " ORDER BY SubmissionTime DESC LIMIT ?";
        PreparedStatement st = db.prepareStatement(sql);
        return st;
    }

    /**
     * Return a prepared SQL statement that, when executed, will
     * return one row for every question of the <i>best</i> submission for the given exercise by the given user.
     * The best submission is the one whose point total is maximal.
     * <p>
     * The rows should be sorted by QuestionId, and each row should contain:
     * - A column named "SubmissionId" with the submission id.
     * - A column named "QuestionId" with the question id,
     * - A column named "Grade" with the grade for that question.
     * - A column named "SubmissionTime" with the time of submission.
     * <p>
     * Parameter 1 of the prepared statement will be set to the User's username, Parameter 2 to the Exercise Id, and
     * Parameter 3 to the number of questions in the given exercise.
     * <p>
     * This will be used by {@link #getBestSubmission(User, Exercise)}
     *
     */
    PreparedStatement getBestSubmissionGradesStatement() throws SQLException {
        // TODO: Implement
        return null;
    }

    /**
     * Return a submission for the given exercise by the given user that satisfies
     * some condition (as defined by an SQL prepared statement).
     * <p>
     * The prepared statement should accept the user name as parameter 1, the exercise id as parameter 2 and a limit on the
     * number of rows returned as parameter 3, and return a row for each question corresponding to the submission, sorted by questionId.
     * <p>
     * Return null if the user has not submitted the exercise (or is not in the database).
     *
     * @param user
     * @param exercise
     * @param stmt
     * @return
     * @throws SQLException
     */
    Submission getSubmission(User user, Exercise exercise, PreparedStatement stmt) throws SQLException {
        stmt.setString(1, user.username);
        stmt.setInt(2, exercise.id);
        stmt.setInt(3, exercise.questions.size());

        ResultSet res = stmt.executeQuery();

        boolean hasNext = res.next();
        if (!hasNext)
            return null;

        int sid = res.getInt("SubmissionId");
        Date submissionTime = new Date(res.getLong("SubmissionTime"));

        float[] grades = new float[exercise.questions.size()];

        for (int i = 0; hasNext; ++i, hasNext = res.next()) {
            grades[i] = res.getFloat("Grade");
        }

        return new Submission(sid, user, exercise, submissionTime, (float[]) grades);
    }

    /**
     * Return the latest submission for the given exercise by the given user.
     * <p>
     * Return null if the user has not submitted the exercise (or is not in the database).
     *
     * @param user
     * @param exercise
     * @return
     * @throws SQLException
     */
    public Submission getLastSubmission(User user, Exercise exercise) throws SQLException {
        return getSubmission(user, exercise, getLastSubmissionGradesStatement());
    }


    /**
     * Return the submission with the highest total grade
     *
     * @param user the user for which we retrieve the best submission
     * @param exercise the exercise for which we retrieve the best submission
     * @return
     * @throws SQLException
     */
    public Submission getBestSubmission(User user, Exercise exercise) throws SQLException {
        return getSubmission(user, exercise, getBestSubmissionGradesStatement());
    }
}
