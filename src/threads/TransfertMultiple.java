package threads;

import java.sql.*;

public class TransfertMultiple {

    private String utilisateur;
    private Connection con;

    public TransfertMultiple(String utilisateur) {
        this.utilisateur = utilisateur;
        try {
            con = DriverManager.getConnection("jdbc:mysql://localhost:8080/transactions",utilisateur,"1234");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void demarrer(String compte_source, String compte_dest, float montant, int iterations, String procedure) {
        Thread t = new Thread(() -> {
            for(int i = 0; i < iterations; ++i){
                try {
                    CallableStatement cs = con.prepareCall("{call " + procedure+ "(?,?,?)}");
                    cs.setString(1, compte_source);
                    cs.setString(2, compte_dest);
                    cs.setFloat(3, montant);
                    cs.executeQuery();
                } catch (SQLException e) {
                    if(e.getSQLState().equals("40001")){
                        //rejouer la transaction
                    }
                }
            }
        });
    }

}
