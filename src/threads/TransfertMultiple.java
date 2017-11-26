package threads;

import java.sql.*;

/**
 * Classe permettant d'invoquer x fois une procédure 'transferer'
 * dans un thread avec un certain niveau d'isolation
 *
 * @author Edward Ransome
 * @author Michael Spierer
 */
public class TransfertMultiple {

    private Connection connection;
    private int nombreBlocages;

    /**
     * Constructeur de TransfertMultiple
     * @param utilisateur un Utilisateur pour se connecter à la DB
     * @param isolation un niveau d'isolation
     */
    public TransfertMultiple(String utilisateur, int isolation) {
        try {
            nombreBlocages=0;

            // Connection à la DB en tant qu'utilisateur
            connection = DriverManager.getConnection("jdbc:mysql://localhost:3306/transactions?noAccessToProcedureBodies=true",utilisateur,"");
            // Pour set @@autocommit=0;
            connection.setAutoCommit(false);
            // Pour spécifier un niveau d'isolation
            connection.setTransactionIsolation(isolation);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Méthode pour démarrer les transferts multiples
     * @param compte_source le compte source du transfert
     * @param compte_dest le compte destinataire du transfert
     * @param montant le montant du transfert
     * @param iterations le nombre de fois que l'on va effectuer le transfert
     * @param procedure la procédure que l'on veut utiliser
     * @return un Thread
     */
    public Thread demarrer(String compte_source, String compte_dest, float montant, int iterations, String procedure) {
        Thread t = new Thread(() -> {
            for(int i = 0; i < iterations; ++i){
                try {
                    // Appelle de la procédure stockée
                    CallableStatement cs = connection.prepareCall("{call " + procedure+ "(?,?,?)}");
                    cs.setString(1, compte_source);
                    cs.setString(2, compte_dest);
                    cs.setFloat(3, montant);
                    cs.execute();
                    if(procedure.equals("transferer1")){
                        //pas de transaction ni d'autocommit donc à faire manuellement
                        connection.commit();
                    }
                } catch (SQLException e) {
                    if(e.getSQLState().equals("40001")){
                        try{
                            nombreBlocages++; //un interblocage a eu lieu
                            connection.rollback();
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

    /**
     * @return le nombre d'interblocage qu'il y a eu après le transfert
     */
    public int getNombreBlocages() {
        return nombreBlocages;
    }
}
