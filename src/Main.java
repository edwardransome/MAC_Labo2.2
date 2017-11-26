import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

public class Main {

    public static void main(String[] args) {
        try {
            Connection con = DriverManager.getConnection("jdbc:mysql://localhost/transactions","root","");
            Statement s =  con.createStatement();
            ResultSet r = s.executeQuery("SELECT * FROM transactions.clients;");


            System.out.println("Les clients sont: ");
            while(r.next()){
                String nom = r.getString("nom");
                System.out.println("nom : "+ nom);
            }
        } catch (Exception e) {
            // TODO: handle exception
            e.printStackTrace();

        }
    }    }
