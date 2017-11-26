import threads.TransfertMultiple;

import java.sql.*;

import static java.sql.Connection.*;

public class Main {

    public static void main(String[] args) {
        doExperience("transferer1", TRANSACTION_READ_COMMITTED);
        doExperience("transferer2", TRANSACTION_READ_COMMITTED);
        doExperience("transferer3", TRANSACTION_READ_COMMITTED);
        doExperience("transferer4", TRANSACTION_READ_COMMITTED);

    }

    private static void doExperience(String procedure, int isolationLevel) {
        TransfertMultiple tm1 = new TransfertMultiple("U1", isolationLevel);
        TransfertMultiple tm2 = new TransfertMultiple("U2", isolationLevel);
        long startTime = System.currentTimeMillis();
        Thread t1 = tm1.demarrer("cpt_a","cpt_b",50,100, procedure);
        Thread t2 = tm2.demarrer("cpt_b", "cpt_a", 50, 100, procedure);
        try {
            t1.join();
            t2.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        long endTime = System.currentTimeMillis();
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
}
