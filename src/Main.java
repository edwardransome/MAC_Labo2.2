import java.sql.*;
import java.util.Properties;


public class Main {


    public static Connection getConnection(String userName, String password, String db, String serverName, int portNumber, String dbName) throws SQLException {

        Connection conn = null;
        Properties connectionProps = new Properties();
        connectionProps.put("user", userName);
        connectionProps.put("password", password);

        if (db.equals("mysql")) {
            conn = DriverManager.getConnection(
                    "jdbc:" + db + "://" +
                            serverName +
                            ":" + portNumber + "/",
                    connectionProps);
        } else if (db.equals("derby")) {
            conn = DriverManager.getConnection(
                    "jdbc:" + db + ":" +
                            dbName +
                            ";create=true",
                    connectionProps);
        }
        System.out.println("Connected to database : " + dbName);
        return conn;
    }

    public static void printUser(Connection conn) throws SQLException {

        Statement s = conn.createStatement();
        ResultSet r = s.executeQuery("SELECT * FROM transactions.clients;");


        System.out.println("Les clients sont: ");
        while (r.next()) {
            String nom = r.getString("nom");
            System.out.println("nom : " + nom);
        }
    }

    public static void printIsolationMode(Connection conn) throws SQLException {

        Statement s = conn.createStatement();
        ResultSet r = s.executeQuery("SELECT @@tx_isolation");


        while (r.next()) {
            System.out.println("Mode d'isolation : "+ r.getString("@@tx_isolation"));
        }

    }

    public static void main(String[] args) {

        try {

            Connection con = getConnection(Constantes.USERNAME_ROOT, Constantes.PASSWORD_ROOT, Constantes.DB, Constantes.SERVER_NAME, Constantes.PORT, Constantes.DB_NAME);
            printIsolationMode(con);
        } catch (Exception e) {
            e.printStackTrace();

        }
    }
}
