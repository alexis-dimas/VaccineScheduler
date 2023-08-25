package scheduler;

import scheduler.db.ConnectionManager;
import scheduler.model.Caregiver;
import scheduler.model.Patient;
import scheduler.model.Vaccine;
import scheduler.util.Util;

import java.awt.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.*;
import java.util.Random;

public class Scheduler {

    // objects to keep track of the currently logged-in user
    // Note: it is always true that at most one of currentCaregiver and currentPatient is not null
    //       since only one user can be logged-in at a time
    private static Caregiver currentCaregiver = null;
    private static Patient currentPatient = null;

    public static void main(String[] args) {
        // printing greetings text
        System.out.println();
        System.out.println("Welcome to the COVID-19 Vaccine Reservation Scheduling Application!");
        System.out.println("*** Please enter one of the following commands ***");
        System.out.println("> create_patient <username> <password>");  //TODO: implement create_patient (Part 1)
        System.out.println("> create_caregiver <username> <password>");
        System.out.println("> login_patient <username> <password>");  // TODO: implement login_patient (Part 1)
        System.out.println("> login_caregiver <username> <password>");
        System.out.println("> search_caregiver_schedule <date>");  // TODO: implement search_caregiver_schedule (Part 2)
        System.out.println("> reserve <date> <vaccine>");  // TODO: implement reserve (Part 2)
        System.out.println("> upload_availability <date>");
        System.out.println("> cancel <appointment_id>");  // TODO: implement cancel (extra credit)
        System.out.println("> add_doses <vaccine> <number>");
        System.out.println("> show_appointments");  // TODO: implement show_appointments (Part 2)
        System.out.println("> logout");  // TODO: implement logout (Part 2)
        System.out.println("> quit");
        System.out.println();

        // read input from user
        BufferedReader r = new BufferedReader(new InputStreamReader(System.in));
        while (true) {
            System.out.print("> ");
            String response = "";
            try {
                response = r.readLine();
            } catch (IOException e) {
                System.out.println("Please try again!");
            }
            // split the user input by spaces
            String[] tokens = response.split(" ");
            // check if input exists
            if (tokens.length == 0) {
                System.out.println("Please try again!");
                continue;
            }
            // determine which operation to perform
            String operation = tokens[0];
            if (operation.equals("create_patient")) {
                createPatient(tokens);
            } else if (operation.equals("create_caregiver")) {
                createCaregiver(tokens);
            } else if (operation.equals("login_patient")) {
                loginPatient(tokens);
            } else if (operation.equals("login_caregiver")) {
                loginCaregiver(tokens);
            } else if (operation.equals("search_caregiver_schedule")) {
                searchCaregiverSchedule(tokens);
            } else if (operation.equals("reserve")) {
                reserve(tokens);
            } else if (operation.equals("upload_availability")) {
                uploadAvailability(tokens);
            } else if (operation.equals("cancel")) {
                cancel(tokens);
            } else if (operation.equals("add_doses")) {
                addDoses(tokens);
            } else if (operation.equals("show_appointments")) {
                showAppointments(tokens);
            } else if (operation.equals("logout")) {
                logout(tokens);
            } else if (operation.equals("quit")) {
                System.out.println("Bye!");
                return;
            } else {
                System.out.println("Invalid operation name!");
            }
        }
    }

    private static void createPatient(String[] tokens) {
        if (tokens.length != 3) {
            System.out.println("Failed to create user.");
            return;
        }

        String username = tokens[1];
        String password = tokens[2];
        if (usernameExistsPatient(username)) {
            System.out.println("Username taken, try again!");
            return;
        }

        byte[] salt = Util.generateSalt();
        byte[] hash = Util.generateHash(password, salt);
        try {
            currentPatient = new Patient.PatientBuilder(username, salt, hash).build();
            currentPatient.saveToDB();
            System.out.println("Created user " + username);
        } catch (SQLException e) {
            System.out.println("Failed to create user.");
            e.printStackTrace();
        }
    }

    private static boolean usernameExistsPatient(String username) {
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();
        String selectUsername = "SELECT * FROM Patients WHERE Username = ?";
        try {
            PreparedStatement statement = con.prepareStatement(selectUsername);
            statement.setString(1, username);
            ResultSet resultSet = statement.executeQuery();
            // returns false if the cursor is not before the first record or if there are no rows in the ResultSet.
            return resultSet.isBeforeFirst();
        } catch (SQLException e) {
            System.out.println("Error occurred when checking username");
            e.printStackTrace();
        } finally {
            cm.closeConnection();
        }
        return true;
    }

    private static void createCaregiver(String[] tokens) {
        // create_caregiver <username> <password>
        // check 1: the length for tokens need to be exactly 3 to include all information (with the operation name)
        if (tokens.length != 3) {
            System.out.println("Failed to create user.");
            return;
        }
        String username = tokens[1];
        String password = tokens[2];
        // check 2: check if the username has been taken already
        if (usernameExistsCaregiver(username)) {
            System.out.println("Username taken, try again!");
            return;
        }
        byte[] salt = Util.generateSalt();
        byte[] hash = Util.generateHash(password, salt);
        // create the caregiver
        try {
            currentCaregiver = new Caregiver.CaregiverBuilder(username, salt, hash).build();
            // save to caregiver information to our database
            currentCaregiver.saveToDB();
            System.out.println("Created user " + username);
        } catch (SQLException e) {
            System.out.println("Failed to create user.");
            e.printStackTrace();
        }
    }

    private static boolean usernameExistsCaregiver(String username) {
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        String selectUsername = "SELECT * FROM Caregivers WHERE Username = ?";
        try {
            PreparedStatement statement = con.prepareStatement(selectUsername);
            statement.setString(1, username);
            ResultSet resultSet = statement.executeQuery();
            // returns false if the cursor is not before the first record or if there are no rows in the ResultSet.
            return resultSet.isBeforeFirst();
        } catch (SQLException e) {
            System.out.println("Error occurred when checking username");
            e.printStackTrace();
        } finally {
            cm.closeConnection();
        }
        return true;
    }

    private static void loginPatient(String[] tokens) {
        if (currentCaregiver != null || currentPatient != null) {
            System.out.println("User already logged in.");
            return;
        }

        if (tokens.length != 3) {
            System.out.println("Login failed.");
            return;
        }

        String username = tokens[1];
        String password = tokens[2];
        Patient patient = null;
        try {
            patient = new Patient.PatientGetter(username, password).get();
        } catch (SQLException e) {
            System.out.println("Login failed.");
            e.printStackTrace();
        }

        if (patient == null) {
            System.out.println("Login failed.");
        } else {
            System.out.println("Logged in as: " + username);
            currentPatient = patient;
        }
    }

    private static void loginCaregiver(String[] tokens) {
        // login_caregiver <username> <password>
        // check 1: if someone's already logged-in, they need to log out first
        if (currentCaregiver != null || currentPatient != null) {
            System.out.println("User already logged in.");
            return;
        }
        // check 2: the length for tokens need to be exactly 3 to include all information (with the operation name)
        if (tokens.length != 3) {
            System.out.println("Login failed.");
            return;
        }
        String username = tokens[1];
        String password = tokens[2];
        Caregiver caregiver = null;
        try {
            caregiver = new Caregiver.CaregiverGetter(username, password).get();
        } catch (SQLException e) {
            System.out.println("Login failed.");
            e.printStackTrace();
        }
        // check if the login was successful
        if (caregiver == null) {
            System.out.println("Login failed.");
        } else {
            System.out.println("Logged in as: " + username);
            currentCaregiver = caregiver;
        }
    }

    private static void searchCaregiverSchedule(String[] tokens) {
        if (currentCaregiver == null && currentPatient == null) {
            System.out.println("Please login first!");
            return;
        }
        // checks if user entered the correct number of inputs
        if (tokens.length != 2) {
            System.out.println("Please try again!");
            return;
        }
        // checks if user entered date format correctly
        char[] chars = tokens[1].toCharArray();
        if (tokens[1].length() != 10 || chars[4] != '-' && chars[7] != '-') {
            System.out.println("Please enter a valid date in format YYYY-MM-DD!");
            return;
        }
        // executes query
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();
        String getAvailabilities = "SELECT DISTINCT A.Username AS Username, " +
                "V.Name AS Name, V.Doses AS Doses " +
                "FROM Availabilities AS A, Vaccines AS V " +
                "WHERE A.Time = ? " +
                "ORDER BY A.Username ";
        try {
            PreparedStatement statement = con.prepareStatement(getAvailabilities);
            statement.setString(1, tokens[1]);
            ResultSet rs = statement.executeQuery();
            // checks for available caregivers
            if (!rs.isBeforeFirst()) {
                System.out.println("No Caregiver is available!");
                return;
            }
            while (rs.next()) {
                System.out.println("[Caregiver: " + rs.getString("Username") +
                        "] [Vaccine: " + rs.getString("Name") + "] [Doses: " +
                        rs.getString("Doses") + "]");
            }
        } catch (SQLException e) {
            System.out.println("Please try again!");
            e.printStackTrace();
        } finally {
            cm.closeConnection();
        }
    }

    private static void reserve(String[] tokens) {
        if (currentCaregiver == null && currentPatient == null) {
            System.out.println("Please login first!");
            return;
        }
        if (currentCaregiver != null) {
            System.out.println("Please login as a patient!");
            return;
        }
        // checks if user entered the correct number of inputs
        if (tokens.length != 3) {
            System.out.println("Please try again!");
            return;
        }
        // checks if user entered date format correctly
        char[] chars = tokens[1].toCharArray();
        if (tokens[1].length() != 10 || chars[4] != '-' && chars[7] != '-') {
            System.out.println("Please enter a valid date in format YYYY-MM-DD!");
            return;
        }

        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();
        try {
            // selects top caregiver available
            String caregiver = "";
            String checkTime = "SELECT TOP 1 Username " +
                    "FROM Availabilities " +
                    "WHERE Time = ? " +
                    "ORDER BY Username ASC ";
            PreparedStatement statement1 = con.prepareStatement(checkTime);
            statement1.setString(1, tokens[1]);
            ResultSet rs1 = statement1.executeQuery();
            if (!rs1.next()) {
                System.out.println("No Caregiver is available!");
                return;
            } else {
                caregiver = rs1.getString("Username");
            }

            // selects vaccine doses
            String checkDoses = "SELECT Doses " +
                    "FROM Vaccines " +
                    "WHERE Name = ? ";
            PreparedStatement statement2 = con.prepareStatement(checkDoses);
            statement2.setString(1, tokens[2]);
            ResultSet rs2 = statement2.executeQuery();
            // checks if inputted vaccine is available
            if (!rs2.next()) {
                System.out.println("Sorry! We do not offer that vaccine!");
                return;
            }
            // checks for available doses
            int numDoses = rs2.getInt("Doses");
            if (numDoses == 0) {
                System.out.println("Not enough available doses!");
                return;
            }

            // deletes availability from caregiver schedule
            String deleteTime = "DELETE FROM Availabilities " +
                    "WHERE Time = ? AND Username = ? ";
            PreparedStatement statement3 = con.prepareStatement(deleteTime);
            statement3.setString(1, tokens[1]);
            statement3.setString(2, caregiver);
            statement3.executeUpdate();

            // decrements vaccine count
            String decrementVaccine = "UPDATE Vaccines " +
                    "SET Doses = Doses - 1 " +
                    "WHERE Name = ? ";
            PreparedStatement statement4 = con.prepareStatement(decrementVaccine);
            statement4.setString(1, tokens[2]);
            statement4.executeUpdate();

            // creates randomly generated ID
            String appointmentID = "";
            Random rand = new Random();
            int num = rand.nextInt(1000);
            appointmentID += Integer.toString(num);
            for (int i = 0; i < 3; i++) {
                appointmentID += (char)('!' + rand.nextInt(93));
            }

            // creates appointment and prints ID and caregiver username
            String createAppointment = "INSERT INTO Appointments " +
                    "(AppointmentID, Time, Vaccine, Patient, Caregiver) " +
                    "VALUES (?, ?, ?, ?, ?) ";
            PreparedStatement statement5 = con.prepareStatement(createAppointment);
            statement5.setString(1, appointmentID);
            statement5.setString(2, tokens[1]);
            statement5.setString(3, tokens[2]);
            statement5.setString(4, currentPatient.getUsername());
            statement5.setString(5, caregiver);
            statement5.executeUpdate();
            System.out.println("[Appointment ID: " + appointmentID +
                    "] [Caregiver username: " + caregiver + "]");
        } catch (SQLException e) {
            System.out.println("Please try again!");
            e.printStackTrace();
        } finally {
            cm.closeConnection();
        }
    }

    private static void uploadAvailability(String[] tokens) {
        // upload_availability <date>
        // check 1: check if the current logged-in user is a caregiver
        if (currentCaregiver == null) {
            System.out.println("Please login as a caregiver first!");
            return;
        }
        // check 2: the length for tokens need to be exactly 2 to include all information (with the operation name)
        if (tokens.length != 2) {
            System.out.println("Please try again!");
            return;
        }
        String date = tokens[1];
        try {
            Date d = Date.valueOf(date);
            currentCaregiver.uploadAvailability(d);
            System.out.println("Availability uploaded!");
        } catch (IllegalArgumentException e) {
            System.out.println("Please enter a valid date!");
        } catch (SQLException e) {
            System.out.println("Error occurred when uploading availability");
            e.printStackTrace();
        }
    }

    private static void cancel(String[] tokens) {
        // TODO: Extra credit
    }

    private static void addDoses(String[] tokens) {
        // add_doses <vaccine> <number>
        // check 1: check if the current logged-in user is a caregiver
        if (currentCaregiver == null) {
            System.out.println("Please login as a caregiver first!");
            return;
        }
        // check 2: the length for tokens need to be exactly 3 to include all information (with the operation name)
        if (tokens.length != 3) {
            System.out.println("Please try again!");
            return;
        }
        String vaccineName = tokens[1];
        int doses = Integer.parseInt(tokens[2]);
        Vaccine vaccine = null;
        try {
            vaccine = new Vaccine.VaccineGetter(vaccineName).get();
        } catch (SQLException e) {
            System.out.println("Error occurred when adding doses");
            e.printStackTrace();
        }
        // check 3: if getter returns null, it means that we need to create the vaccine and insert it into the Vaccines
        //          table
        if (vaccine == null) {
            try {
                vaccine = new Vaccine.VaccineBuilder(vaccineName, doses).build();
                vaccine.saveToDB();
            } catch (SQLException e) {
                System.out.println("Error occurred when adding doses");
                e.printStackTrace();
            }
        } else {
            // if the vaccine is not null, meaning that the vaccine already exists in our table
            try {
                vaccine.increaseAvailableDoses(doses);
            } catch (SQLException e) {
                System.out.println("Error occurred when adding doses");
                e.printStackTrace();
            }
        }
        System.out.println("Doses updated!");
    }

    private static void showAppointments(String[] tokens) {
        if (currentCaregiver == null && currentPatient == null) {
            System.out.println("Please login first!");
            return;
        }

        // checks if user entered the correct number of inputs
        if (tokens.length != 1) {
            System.out.println("Please try again!");
            return;
        }

        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();
        // shows logged in caregiver their appointments
        if (currentCaregiver != null) {
            try {
                String showCaregiver = "SELECT AppointmentID, Vaccine, " +
                        "Time, Patient " +
                        "FROM Appointments " +
                        "WHERE Caregiver = ? " +
                        "ORDER BY AppointmentID";
                PreparedStatement statement = con.prepareStatement(showCaregiver);
                statement.setString(1, currentCaregiver.getUsername());
                ResultSet rs = statement.executeQuery();
                ResultSetMetaData rsmd = rs.getMetaData();
                int numColumns = rsmd.getColumnCount();
                // checks if there are scheduled appointments
                if (!rs.isBeforeFirst()) {
                    System.out.println("No scheduled appointments!");
                    return;
                }
                System.out.println("AppointmentID Vaccine Date Patient");
                while (rs.next()) {
                    for (int i = 1; i <= numColumns; i++) {
                        System.out.print(rs.getString(i) + " ");
                    }
                    System.out.println();
                }
            } catch (SQLException e) {
                System.out.println("Please try again!");
                e.printStackTrace();
            } finally {
                cm.closeConnection();
            }
            return;
        }

        // shows logged in patient their appointments
        if (currentPatient != null) {
            try {
                String showPatient = "SELECT AppointmentID, Vaccine, " +
                        "Time, Caregiver " +
                        "FROM Appointments " +
                        "WHERE Patient = ? " +
                        "ORDER BY AppointmentID";
                PreparedStatement statement = con.prepareStatement(showPatient);
                statement.setString(1, currentPatient.getUsername());
                ResultSet rs = statement.executeQuery();
                ResultSetMetaData rsmd = rs.getMetaData();
                int numColumns = rsmd.getColumnCount();
                // checks if there are scheduled appointments
                if (!rs.isBeforeFirst()) {
                    System.out.println("No scheduled appointments!");
                    return;
                }
                System.out.println("AppointmentID Vaccine Date Caregiver");
                while (rs.next()) {
                    for (int i = 1; i <= numColumns; i++) {
                        System.out.print(rs.getString(i) + " ");
                    }
                    System.out.println();
                }
            } catch (SQLException e) {
                System.out.println("Please try again!");
                e.printStackTrace();
            } finally {
                cm.closeConnection();
            }
        }
    }

    private static void logout(String[] tokens) {
        if (currentCaregiver == null && currentPatient == null) {
            System.out.println("Please login first!");
            return;
        }
        // checks if user entered the correct number of inputs
        if (tokens.length != 1) {
            System.out.println("Please try again!");
            return;
        }
        currentCaregiver = null;
        currentPatient = null;
        System.out.println("Successfully logged out!");
    }
}