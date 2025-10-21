CREATE OR REPLACE FUNCTION transfer_storeExternalCode()
RETURNS void AS $$
BEGIN
    INSERT INTO target_edw.storeExternalCode (sourceID, externalID, partnerStoreID)
    SELECT
        '001' AS sourceID,
        ps.id AS externalID,
        ps_edw.id AS partnerStoreID
    FROM loading_automatica.project_store ps
    JOIN target_edw.partnerStore ps_edw ON ps.latitude = ps_edw.latitude
                                        AND ps.longitude = ps_edw.longitude;
END;
$$ LANGUAGE plpgsql

SELECT transfer_storeExternalCode();

CREATE OR REPLACE FUNCTION transfer_partnerStore()
RETURNS void AS $$
BEGIN
    INSERT INTO target_edw.partnerStore (address, latitude, longitude, statusID, storeTitleID, partnerID)
    SELECT
        ps.address,
        ps.latitude,
        ps.longitude,
        ps.active AS statusID,
        ps.store_title_id AS storeTitleID
    FROM loading_automatics.project_store ps;
END;
$$ LANGUAGE plpgsql;

SELECT transfer_partnerStore();


CREATE OR REPLACE FUNCTION transfer_partnerStore()
RETURNS void AS $$
BEGIN
    INSERT INTO target_edw.partnerStore (address, latitude, longitude, statusID, storeTitleID, partnerID)
    SELECT
        ps.address,
        ps.latitude,
        ps.longitude,
        ps.active AS statusID,
        pst.id AS storeTitleID,
        p.id AS partnerID 
    FROM loading_automatics.project_store ps
    JOIN loading_automatics.project_storetitle pst ON ps.store_title_id = pst.id
    JOIN target_edw.storeTitle st ON pst.name = st.name
    JOIN target_edw.partner p ON p.titleID = st.id AND p.name = pst.name;
END;
$$ LANGUAGE plpgsql;


CREATE OR REPLACE FUNCTION transfer_storeVisit()
RETURNS void AS $$
BEGIN
    INSERT INTO target_edw.storeVisit (partnerStoreID, visitDate, cancelReasonID, cancelReasonText, fakeID, externalID, sourceID)
    SELECT
        ps.id AS partnerStoreID,
        pc.date AS visitDate,
        pc.cancel_reason AS cancelReasonID,
        pc.cancel_reason_text AS cancelReasonText,
        pc.fake AS fakeID,
        pc.id AS externalID,
        '001' AS sourceID
    FROM loading_automatics.project_call pc
    JOIN target_edw.partnerStore ps ON ps.externalID = pc.store_id;
END;
$$ LANGUAGE plpgsql;


CREATE OR REPLACE FUNCTION transfer_storePromoCalendar()
RETURNS void AS $$
BEGIN
    INSERT INTO target_edw.storePromoCalendar (partnerStoreID, calendarTypeID, startDate, endDate, posmDate)
    SELECT
        sec.partnerStoreID,
        ppc.type AS calendarTypeID,
        ppc.start AS startDate,
        ppc.end AS endDate,
        ppc.posm_date AS posmDate
    FROM loading_automatics.project_promocalendar ppc
    JOIN target_edw.storeExternalCode sec ON sec.externalID = ppc.store_id AND sec.sourceID = '001';
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION transfer_storeVisitPlan()
RETURNS void AS $$
BEGIN
    INSERT INTO target_edw.storeVisitPlan (visitDate, partnerStoreID, storeVisitID)
    SELECT
        pp.date AS visitDate,
        sec.partnerStoreID,
        sv.id AS storeVisitID
    FROM loading_automatics.project_plan pp
    JOIN target_edw.storeExternalCode sec ON sec.externalID = pp.store_id AND sec.sourceID = '001'
    JOIN target_edw.storeVisit sv ON sv.externalID = pp.call_id AND sv.sourceID = '001';
END;
$$ LANGUAGE plpgsql;


