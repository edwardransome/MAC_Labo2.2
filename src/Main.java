import threads.TransfertMultiple;

import java.sql.*;

public class Main {


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
            TransfertMultiple unTransfertMultiple = new TransfertMultiple("U1","");
            unTransfertMultiple.demarrer("","",50,100,"transferer1");
        } catch (Exception e) {
            e.printStackTrace();

        }
    }
}
