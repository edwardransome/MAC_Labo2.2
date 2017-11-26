import java.sql.*;

public class Experience {
    private boolean interblocage;
    private long duree;
    private boolean coherence;
    private String procedure;
    private int iterations;

    public Experience(int iterations, String procedure){
        this.iterations=iterations;
        this.procedure=procedure;
    }

    public void faireExperience(String niveauIsolation){
        try {
            Connection con = DriverManager.getConnection("jdbc:mysql://localhost:3306/transactions?noAccessToProcedureBodies=true","root","");
            //On set l'autocommit a 0
            Statement s = con.createStatement();
            s.executeQuery("set @@autocommit=0;");

            //On set le niveau d'isolation
            s = con.createStatement();
            s.executeQuery("SET SESSION TRANSACTION ISOLATION LEVEL "+niveauIsolation);

            afficheNiveauIsolation(con);

            //On recupere les montants de départs des deux comptes
        } catch (SQLException e) {
            e.printStackTrace();
        }

        long debut = System.currentTimeMillis();

        long fin = System.currentTimeMillis();
        this.duree= (fin - debut);
    }


    public static void afficheNiveauIsolation(Connection conn) throws SQLException {

        Statement s = conn.createStatement();
        ResultSet r = s.executeQuery("SELECT @@tx_isolation");

        while (r.next()) {
            System.out.println("Mode d'isolation : "+ r.getString("@@tx_isolation"));
        }

    }
}
