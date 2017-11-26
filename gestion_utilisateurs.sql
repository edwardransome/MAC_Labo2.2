DROP DATABASE IF EXISTS transactions;
CREATE DATABASE transactions;
USE transactions;
SET foreign_key_checks = 0;

CREATE TABLE clients (
    id INT NOT NULL AUTO_INCREMENT,
    nom VARCHAR(30),
    PRIMARY KEY (id)
);

CREATE TABLE comptes (
    id INT NOT NULL AUTO_INCREMENT,
    no VARCHAR(30),
    solde FLOAT DEFAULT 0.0,
    min_autorise FLOAT DEFAULT 0,
    max_retrait_journalier FLOAT DEFAULT 1000,
    blocage BOOLEAN DEFAULT FALSE,
    proprietaire INT NOT NULL,
    PRIMARY KEY (id),
    FOREIGN KEY (proprietaire)
        REFERENCES clients (id),
    UNIQUE (no)
);

CREATE TABLE acces_a_compte (
	id_client INT,
    id_compte INT,
    droit_lecture_ecriture INT DEFAULT 2,
    PRIMARY KEY (id_client, id_compte),
    FOREIGN KEY (id_client) REFERENCES clients (id),
    FOREIGN KEY (id_compte) REFERENCES comptes (id)
);

CREATE TABLE journal (
	id INT NOT NULL AUTO_INCREMENT,
	date_operation DATE,
    id_compte INT NOT NULL,
    id_client INT NOT NULL,
    ecriture BOOLEAN NOT NULL, ## si ecriture vaut true, on est en ecriture et s'il vaut false, en lecture
	authorisation INT NOT NULL CHECK (authorisation BETWEEN 0 AND 4),
	solde_initial FLOAT,
	solde_resultant FLOAT,
    PRIMARY KEY (id),
    FOREIGN KEY (id_client) REFERENCES clients (id),
    FOREIGN KEY (id_compte) REFERENCES comptes (id)

);

DELIMITER $$
CREATE PROCEDURE deposer_argent(IN id_compte_dest INT, IN somme FLOAT)
BEGIN
	DECLARE EXIT HANDLER FOR SQLEXCEPTION 
    BEGIN
          ROLLBACK;
          RESIGNAL;
    END;

    START TRANSACTION;
        SELECT USER() INTO @utilisateur_courant;
		SELECT SUBSTRING(@utilisateur_courant, 1, 2) INTO @nom;
        SELECT id FROM clients WHERE nom = @nom INTO @id_client_actuel;
        SELECT blocage FROM comptes WHERE id = id_compte_dest INTO @blocage;
		SELECT COUNT(*) FROM acces_a_compte WHERE id_client = @id_client_actuel AND id_compte = id_compte_dest INTO @existe;
        IF(@existe = 0) THEN
			SIGNAL SQLSTATE '45000' SET message_text = 'Vous n\'avez pas de droits sur ce compte ou il n\'existe pas';
		ELSE
			IF(@blocage) THEN 
				SIGNAL SQLSTATE '45000' SET message_text = 'Votre compte est bloqué';
			END IF;
			SELECT droit_lecture_ecriture FROM acces_a_compte WHERE id_client = @id_client_actuel AND id_compte = id_compte_dest INTO @droits;
			IF (@droits = 2) THEN
				
				SELECT solde FROM comptes WHERE id = id_compte_dest into @solde;
				UPDATE comptes SET solde = solde + somme WHERE id = id_compte_dest;
		
				INSERT INTO journal( id_compte,id_client,date_operation,ecriture, authorisation,solde_initial,solde_resultant)
				VALUES( id_compte_dest,@id_client_actuel,SYSDATE(),True,0,@solde,@solde+somme);
                
			ELSE
            	INSERT INTO journal( id_compte,id_client,date_operation,ecriture, authorisation)
				VALUES( id_compte_dest,@id_client_actuel,SYSDATE(),True,4);
                
				SIGNAL SQLSTATE '45000' SET message_text = 'Vous n\'avez pas les droits nécessaires pour cette opération sur ce compte';
            END IF;
		END IF;
    COMMIT;
END
$$
DELIMITER ;


DELIMITER $$
CREATE PROCEDURE lire_solde(IN id_compte_dest INT)
BEGIN
	DECLARE EXIT HANDLER FOR SQLEXCEPTION 
    BEGIN
		ROLLBACK;
		RESIGNAL;

    END;

    START TRANSACTION;
        SELECT USER() INTO @utilisateur_courant;
		SELECT SUBSTRING(@utilisateur_courant, 1, 2) INTO @nom;
        SELECT id FROM clients WHERE nom = @nom INTO @id_client_actuel;
		SELECT COUNT(*) FROM acces_a_compte WHERE id_client = @id_client_actuel AND id_compte = id_compte_dest INTO @existe;
        IF(@existe = 0) THEN
			SIGNAL SQLSTATE '45000' SET message_text = 'Vous n\'avez pas de droits sur ce compte ou il n\'existe pas';
            SELECT 1;
		ELSE
			SELECT droit_lecture_ecriture FROM acces_a_compte WHERE id_client = @id_client_actuel AND id_compte = id_compte_dest INTO @droits;
			IF (@droits > 0) THEN
				        
				INSERT INTO journal( id_compte,id_client,date_operation,ecriture, authorisation)
				VALUES( id_compte_dest,@id_client_actuel,SYSDATE(),False,0);
        
				SELECT solde FROM comptes WHERE id = id_compte_dest;
			ELSE
				SIGNAL SQLSTATE '45000' SET message_text = 'Vous n\'avez pas les droits nécessaires pour cette opération sur ce compte';
			END IF;
		END IF;
    COMMIT;
END
$$
DELIMITER ;


DELIMITER $$
CREATE TRIGGER verification_compte_vide BEFORE DELETE ON comptes 
FOR EACH ROW BEGIN
    IF(OLD.solde > 0) THEN 
          SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Ce compte n\'est pas vide! Impossible de le supprimer'; 
	END IF;
END
$$
DELIMITER ;

## User creation
CREATE USER IF NOT EXISTS admin@'localhost', U1@'localhost', U2@'localhost', U3@'localhost';
CREATE USER IF NOT EXISTS admin@'%', U1@'%', U2@'%', U3@'%';

## Droits d'administrateur
GRANT ALL
ON clients
TO admin@'localhost', admin@'%';

## Droit d'execution des procedures stockees
GRANT EXECUTE
ON PROCEDURE deposer_argent
TO U1@'localhost', U1@'%', U2@'localhost', U2@'%', U3@'localhost', U3@'%'; 

GRANT EXECUTE
ON PROCEDURE lire_solde
TO U1@'localhost', U1@'%', U2@'localhost', U2@'%', U3@'localhost', U3@'%'; 

## Creation des deux comptes et trois clients
INSERT INTO clients (nom) VALUES("U1"),("U2"),("U3");
INSERT INTO comptes (no,solde,proprietaire) VALUES("1234",0,1),("4321",0,1);
INSERT INTO acces_a_compte (id_client,id_compte, droit_lecture_ecriture) VALUES(1,1,2),(2,1,2),(1,2,2),(3,2,1);

SELECT * from clients;
SELECT * from comptes;
SELECT * from journal;

#Labo 2 Etape 2

SET @@autocommit=0;

DELIMITER $$
CREATE PROCEDURE transferer1(IN cpt1 VARCHAR(30), IN cpt2 VARCHAR(30), IN montant FLOAT)
BEGIN
	DECLARE etat FLOAT DEFAULT 0.0;
    SELECT solde FROM comptes WHERE no = cpt1 INTO etat;
    SET etat = etat - montant;
    UPDATE comptes SET solde = etat WHERE no = cpt1;
    
    SELECT solde FROM comptes WHERE no = cpt2 INTO etat;
    SET etat = etat + montant;
    UPDATE comptes SET solde = etat WHERE no = cpt2;
END
$$
DELIMITER ;

DELIMITER $$
CREATE PROCEDURE transferer2(IN cpt1 VARCHAR(30), IN cpt2 VARCHAR(30), IN montant FLOAT)
BEGIN
	DECLARE etat FLOAT DEFAULT 0.0;
	START TRANSACTION;
		SELECT solde FROM comptes WHERE no = cpt1 INTO etat;
		SET etat = etat - montant;
		UPDATE comptes SET solde = etat WHERE no = cpt1;
		
		SELECT solde FROM comptes WHERE no = cpt2 INTO etat;
		SET etat = etat + montant;
		UPDATE comptes SET solde = etat WHERE no = cpt2;
    COMMIT;
END
$$
DELIMITER ;

DELIMITER $$
CREATE PROCEDURE transferer3(IN cpt1 VARCHAR(30), IN cpt2 VARCHAR(30), IN montant FLOAT)
BEGIN
		DECLARE etat FLOAT DEFAULT 0.0;
	START TRANSACTION;
        SELECT solde FROM comptes WHERE no = cpt1 LOCK IN SHARE MODE;
		SELECT solde FROM comptes WHERE no = cpt1 INTO etat;
		SET etat = etat - montant;
		UPDATE comptes SET solde = etat WHERE no = cpt1;
		
        SELECT solde FROM comptes WHERE no = cpt2 LOCK IN SHARE MODE;
		SELECT solde FROM comptes WHERE no = cpt2 INTO etat;
		SET etat = etat + montant;
		SELECT solde FROM comptes WHERE no = cpt2 FOR UPDATE;
		UPDATE comptes SET solde = etat WHERE no = cpt2;
    COMMIT;
END
$$
DELIMITER ;


## On va ordonner les comptes par ordre alphabetique et verouiller dans cet ordre pour
## éviter l'interblocage.
DELIMITER $$
CREATE PROCEDURE transferer4(IN cpt1 VARCHAR(30), IN cpt2 VARCHAR(30), IN montant FLOAT)
BEGIN
	DECLARE etat FLOAT DEFAULT 0.0;
    
	START TRANSACTION;
		IF (STRCMP(cpt1, cpt2) < 0) THEN
        #cpt1 doit être accédé en premier
			SELECT solde FROM comptes WHERE no = cpt1 LOCK IN SHARE MODE;
			SELECT solde FROM comptes WHERE no = cpt1 INTO etat;
			SET etat = etat - montant;
			UPDATE comptes SET solde = etat WHERE no = cpt1;
			
			SELECT solde FROM comptes WHERE no = cpt2 LOCK IN SHARE MODE;
			SELECT solde FROM comptes WHERE no = cpt2 INTO etat;
			SET etat = etat + montant;
			SELECT solde FROM comptes WHERE no = cpt2 FOR UPDATE;
			UPDATE comptes SET solde = etat WHERE no = cpt2;
		ELSE
        #cpt2 doit être accédé en premier
			SELECT solde FROM comptes WHERE no = cpt2 LOCK IN SHARE MODE;
			SELECT solde FROM comptes WHERE no = cpt2 INTO etat;
			SET etat = etat + montant;
			SELECT solde FROM comptes WHERE no = cpt2 FOR UPDATE;
			UPDATE comptes SET solde = etat WHERE no = cpt2;
        
			SELECT solde FROM comptes WHERE no = cpt1 LOCK IN SHARE MODE;
			SELECT solde FROM comptes WHERE no = cpt1 INTO etat;
			SET etat = etat - montant;
			UPDATE comptes SET solde = etat WHERE no = cpt1;
		END IF;
			
    COMMIT;
END
$$
DELIMITER ;

GRANT EXECUTE
ON PROCEDURE transferer1
TO U1@'localhost', U1@'%', U2@'localhost', U2@'%';

GRANT EXECUTE
ON PROCEDURE transferer2
TO U1@'localhost', U1@'%', U2@'localhost', U2@'%';

GRANT EXECUTE
ON PROCEDURE transferer3
TO U1@'localhost', U1@'%', U2@'localhost', U2@'%';

GRANT EXECUTE
ON PROCEDURE transferer4
TO U1@'localhost', U1@'%', U2@'localhost', U2@'%';

