import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

public class Main {

    public static void main(String[] args) {
        try {
            Connection con = DriverManager.getConnection("jdbc:mysql://localhost/banque","root","");
            Statement s =  con.createStatement();
            ResultSet r = s.executeQuery("SELECT * FROM banque.mouvement_log;");


            System.out.println("Les enrengistrement sont: ");
            while(r.next()){
                String montant = r.getString("montant");
                System.out.println("montant : "+ montant);
            }
        } catch (Exception e) {
            // TODO: handle exception
            e.printStackTrace();

        }
    }    }
