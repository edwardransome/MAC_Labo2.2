package threads;

import java.sql.*;

public class TransfertMultiple {

    private String utilisateur;
    private Connection con;
    private int nombreBlocages;

    public TransfertMultiple(String utilisateur, int isolation) {
        this.utilisateur = utilisateur;
        try {
            con = DriverManager.getConnection("jdbc:mysql://localhost:3306/transactions?noAccessToProcedureBodies=true",utilisateur,"");
            con.setAutoCommit(false);

            con.setTransactionIsolation(isolation);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public Thread demarrer(String compte_source, String compte_dest, float montant, int iterations, String procedure) {
        Thread t = new Thread(() -> {
            for(int i = 0; i < iterations; ++i){
                try {
                    CallableStatement cs = con.prepareCall("{call " + procedure+ "(?,?,?)}");
                    cs.setString(1, compte_source);
                    cs.setString(2, compte_dest);
                    cs.setFloat(3, montant);
                    cs.execute();
                    if(procedure.equals("transferer1")){
                        //pas de transaction ni d'autocommit donc à faire manuellement
                        con.commit();
                    }
                } catch (SQLException e) {
                    if(e.getSQLState().equals("40001")){
                        try{
                            nombreBlocages++; //un interblocage a eu lieu
                            con.rollback();
                            i--; //on ajoute une itération vu que celle ci à échouée.
                        }catch(SQLException sqle){
                            sqle.printStackTrace();
                        }
                    }
                }
            }
        });
        t.start();
        return t;
    }

    public int getNombreBlocages() {
        return nombreBlocages;
    }
}
