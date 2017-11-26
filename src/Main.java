import threads.TransfertMultiple;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.*;
import java.util.Arrays;
import java.util.stream.Collectors;

import static java.sql.Connection.*;

/**
 * Classe principal permettant de tester les quatre niveaux d'isolation
 * pour chacune des procédures 'transferer' permettant de transferer de l'argent
 * de la base de données 'transactions'et écrit les résultats des tests dans un fichier csv
 *
 * @author Edward Ransome
 * @author Michael Spierer
 */
public class Main {

    static final float MONTANT = 50;
    static final int NB_ITERATIONS = 2000;
    static final String U1 = "U1";
    static final String U2 = "U1";
    static final String COMPTE_1 = "cpt_a";
    static final String COMPTE_2 = "cpt_b";
    static final String PATH_RESULT = "../result.csv";

    static Connection connection;
    static PrintWriter printWriter;

    //Initialisation des variables statiques non finales dans un bloc static
    static {
        try {
            //creation d'une connexion à la db static pour les vérifications des montants d'un compte
            connection = DriverManager.getConnection("jdbc:mysql://localhost:3306/transactions?noAccessToProcedureBodies=true","root","1234");

            //creation d'un writer pour écrire les résultats des expériences en format csv
            printWriter  = new PrintWriter(new FileWriter(PATH_RESULT));

        } catch (SQLException e) {
            e.printStackTrace();
        } catch (IOException e){
            e.printStackTrace();
        }
    }


    public static void main(String[] args) {

        doAllExperiences("transferer1");
        doAllExperiences("transferer2");
        doAllExperiences("transferer3");
        doAllExperiences("transferer4");

        printWriter.close();
    }

    /**
     * Fait la meme procédure pour chaque niveau d'isolation
     * @param procedure la procedure a executer
     */
    private static void doAllExperiences(String procedure) {
        doExperience(procedure, TRANSACTION_READ_UNCOMMITTED);
        doExperience(procedure, TRANSACTION_READ_COMMITTED);
        doExperience(procedure, TRANSACTION_REPEATABLE_READ);
        doExperience(procedure, TRANSACTION_SERIALIZABLE);
    }

    /**
     * Fait une expérience avec une procédure et un niveau d'isolation spécifique
     * @param procedure la procedure a executer
     * @param niveauIsolation le niveau d'isolation
     */
    private static void doExperience(String procedure, int niveauIsolation) {

        float montantDepartCompte1 = 0;
        float montantDepartCompte2 = 0;
        float montantFinCompte1 = 0;
        float montantFinCompte2 = 0;

        //On recupere les montants initiaux des deux comptes (afin de verifier plus tard s'il y a cohérence)
        try {
             montantDepartCompte1 = getMontantDB(COMPTE_1) ;
             montantDepartCompte2 = getMontantDB(COMPTE_2) ;
        } catch (SQLException e) {
            e.printStackTrace();
        }

        TransfertMultiple tm1 = new TransfertMultiple(U1, niveauIsolation);
        TransfertMultiple tm2 = new TransfertMultiple(U2, niveauIsolation);

        long startTime = System.currentTimeMillis();

        //On start les deux objects actifs
        Thread t1 = tm1.demarrer(COMPTE_1,COMPTE_2,MONTANT,NB_ITERATIONS, procedure);
        Thread t2 = tm2.demarrer(COMPTE_2, COMPTE_1, MONTANT, NB_ITERATIONS, procedure);
        try {
            t1.join();
            t2.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        long endTime = System.currentTimeMillis();
        long duree = (endTime-startTime);

        //On récupère les montants finaux des deux comptes
        try {
             montantFinCompte1 = getMontantDB(COMPTE_1);
             montantFinCompte2 = getMontantDB(COMPTE_2);

        } catch (SQLException e) {
            e.printStackTrace();
        }

        //On vérifie que la somme des montants de départ correspond a celle des montants de fins
        boolean coherence = (montantDepartCompte1+montantDepartCompte2) == (montantFinCompte1 + montantFinCompte2);

        //Ecriture des resultats dans le fichier csv en format csv
        String data[] = {procedure,
                String.valueOf(niveauIsolation),
                String.valueOf(NB_ITERATIONS),
                String.valueOf(duree),
                String.valueOf(tm1.getNombreBlocages()),
                String.valueOf(coherence)};

        String dataCSV = Arrays.stream(data).map(Object::toString).collect(Collectors.joining(","));
        printWriter.println(dataCSV);
    }

    /**
     * Récupère le solde d'un compte passé en paramètre
     * @param compte le compte dont on souhaite voir le solde
     * @return le solde du compte
     * @throws SQLException si la query n'est pas executée
     */
    public static float getMontantDB(String compte) throws SQLException {

        Statement s = connection.createStatement();
        ResultSet r = s.executeQuery("SELECT solde FROM transactions.comptes WHERE comptes.no = \""+compte+"\";");

        float montant = 0;
        while (r.next()) {
            montant = r.getFloat("solde");
        }
        return montant;
    }
}
