CREATE OR REPLACE FUNCTION insert_data_partnerstore()
RETURNS void AS $$
BEGIN
    -- Соединение с базой данных Automatica
    PERFORM dblink_connect(
        'automatica_conn',
        'hostaddr=campari.automatica.ly dbname=campari user=campari_tedo password=ag_o3iIa4dsO port=3306'
    );

    -- Вставка данных в target_edw.partnerstore
    PERFORM dblink_exec(
        'automatica_conn',
        $sql$
        INSERT INTO target_edw.partnerstore (address, latitude, longitude, statusID, storeTitleID, partnerID)
        SELECT 
            ps.address,
            ps.latitude,
            ps.longitude,
            ds.id AS statusID,
            dt.storeTitleID,
            p.id AS partnerID
        FROM 
            project_store ps
        JOIN 
            (SELECT externalID, id FROM dictionarystatus WHERE sourceID = 1) ds ON ps.active = ds.externalID
        JOIN 
            (SELECT externalID, storeTitleID FROM dictionarytitle WHERE sourceID = 1) dt ON ps.store_title_id = dt.externalID
        LEFT JOIN 
            partner p ON p.address = ps.address
        $sql$
    );

    -- Закрытие соединения
    PERFORM dblink_disconnect('automatica_conn');
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION insert_data_storeExternalCode()
RETURNS void AS $$
BEGIN
    -- Соединение с базой данных Automatica
    PERFORM dblink_connect(
        'automatica_conn',
        'hostaddr=campari.automatica.ly dbname=campari user=campari_tedo password=ag_o3iIa4dsO port=3306'
    );

    -- Вставка данных в target_edw.storeExternalCode
    PERFORM dblink_exec(
        'automatica_conn',
        $sql$
        INSERT INTO storeExternalCode (sourceID, externalID, partnerStoreID)
        SELECT 
            1 AS sourceID,
            ps.id AS externalID,
            p.id AS partnerStoreID
        FROM 
            project_store ps
        LEFT JOIN 
            partner p ON p.address = ps.address
        $sql$
    );

    -- Закрытие соединения
    PERFORM dblink_disconnect('automatica_conn');
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION insert_data_dictionaryTitle()
RETURNS void AS $$
BEGIN
    -- Соединение с базой данных Automatica
    PERFORM dblink_connect(
        'automatica_conn',
        'hostaddr=campari.automatica.ly dbname=campari user=campari_tedo password=ag_o3iIa4dsO port=3306'
    );

    -- Вставка данных в target_edw.dictionaryTitle
    PERFORM dblink_exec(
        'automatica_conn',
        $sql$
        INSERT INTO dictionaryTitle (name, statusID, externalID, sourceID)
        SELECT 
            pst.name,
            2 AS statusID,
            pst.id AS externalID,
            1 AS sourceID
        FROM 
            project_storetitle pst
        $sql$
    );

    -- Закрытие соединения
    PERFORM dblink_disconnect('automatica_conn');
END;
$$ LANGUAGE plpgsql;

SELECT insert_data_partnerstore();
SELECT insert_data_storeExternalCode();
SELECT insert_data_dictionaryTitle();
