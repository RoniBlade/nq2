CREATE TABLE storeFacingPlan (
    id BIGINT NOT NULL,
    facingNumber DATE,
    partnerStoreId INTEGER,
    brandID INTEGER,
    CONSTRAINT pk_storeFacingPlan PRIMARY KEY (id)
);

CREATE TABLE dictionaryBrand (
    id BIGINT NOT NULL,
    name VARCHAR(255) NOT NULL,
    statusID INTEGER,
    CONSTRAINT pk_dictionaryBrand PRIMARY KEY (id)
);

CREATE TABLE storeVisitPlan (
    id BIGINT NOT NULL,
    visitDate DATE,
    partnerStoreId INTEGER,
    storeVisitID INTEGER,
    CONSTRAINT pk_storeVisitPlan PRIMARY KEY (id)
);

CREATE TABLE partnerFunctions (
    partnerStoreID INT NOT NULL,
    partnerID INT NOT NULL,
    partnerFunction VARCHAR(255) NOT NULL,
    PRIMARY KEY (partnerStoreID, partnerID, partnerFunction)
);

CREATE TABLE storePromoCalendar (
    id BIGINT NOT NULL,
    partnerStoreID INTEGER NOT NULL,
    calendarTypeID INTEGER NOT NULL,
    startDate DATE NOT NULL,
    endDate DATE NOT NULL,
    posmDate DATE,
    statusID INTEGER NOT NULL,
    CONSTRAINT pk_storePromoCalendar PRIMARY KEY (id)
);

CREATE TABLE storeVisit (
    id BIGINT NOT NULL,
    partnerStoreID INTEGER NOT NULL,
    visitDate DATE NOT NULL,
    cancelReasonID INTEGER,
    cancelReasonText TEXT,
    fakeID INTEGER,
    externalID INTEGER,
    sourceID INTEGER,
    CONSTRAINT pk_storeVisit PRIMARY KEY (id)
);

CREATE TABLE offTake (
    id BIGINT NOT NULL,
    chainUnitID INTEGER NOT NULL,
    year INTEGER NOT NULL,
    month INTEGER NOT NULL,
    skuID INTEGER NOT NULL,
    saleLitters DECIMAL(10, 2) NOT NULL,
    statusID INTEGER NOT NULL,
    CONSTRAINT pk_offTake PRIMARY KEY (id)
);

CREATE TABLE dictionarySku (
    id BIGINT NOT NULL,
    name VARCHAR(255) NOT NULL,
    litters DECIMAL(10, 2),
    statusID INTEGER NOT NULL,
    CONSTRAINT pk_dictionarySku PRIMARY KEY (id)
);

CREATE TABLE agency (
    id BIGINT NOT NULL,
    name VARCHAR(255) NOT NULL,
    statusID INTEGER,
    CONSTRAINT pk_agency PRIMARY KEY (id)
);

CREATE TABLE partnerStore (
    id BIGINT NOT NULL,
    partnerID INTEGER NOT NULL,
    unitFormatID INTEGER NOT NULL,
    regionID INTEGER NOT NULL,
    address VARCHAR(255) NOT NULL,
    statusID INTEGER NOT NULL,
    latitude DECIMAL(9, 6),
    longitude DECIMAL(9, 6),
    storeTitleID INTEGER NOT NULL,
    CONSTRAINT pk_partnerStore PRIMARY KEY (id)
);

CREATE TABLE dictionaryStoreFormat (
    id BIGINT NOT NULL,
    name VARCHAR(255) NOT NULL,
    brief VARCHAR(255),
    statusID INTEGER NOT NULL,
    CONSTRAINT pk_dictionaryStoreFormat PRIMARY KEY (id)
);

CREATE TABLE dictionarySalesChannel (
    id BIGINT NOT NULL,
    name VARCHAR(255) NOT NULL,
    statusID INTEGER NOT NULL,
    CONSTRAINT pk_dictionarySalesChannel PRIMARY KEY (id)
);

CREATE TABLE dictionaryDeliveryZone (
    id BIGINT NOT NULL,
    name VARCHAR(255) NOT NULL,
    statusID INTEGER NOT NULL,
    CONSTRAINT pk_dictionaryDeliveryZone PRIMARY KEY (id)
);

CREATE TABLE deliveryZoneList (
    partnerID BIGINT NOT NULL,
    zoneID INTEGER NOT NULL,
    CONSTRAINT pk_deliveryZoneList PRIMARY KEY (id)
);

CREATE TABLE dictionaryPartnerType (
    id BIGINT NOT NULL,
    name VARCHAR(255) NOT NULL,
    statusID INTEGER NOT NULL,
    CONSTRAINT pk_dictionaryPartnerType PRIMARY KEY (id)
);

CREATE TABLE dictionaryBusinessRegion (
    id BIGINT NOT NULL,
    name VARCHAR(255) NOT NULL,
    statusID INTEGER NOT NULL,
    CONSTRAINT pk_dictionaryBusinessRegion PRIMARY KEY (id)
);

CREATE TABLE dictionarySource (
    id BIGINT NOT NULL,
    name VARCHAR(255) NOT NULL,
    statusID INTEGER NOT NULL,
    CONSTRAINT pk_dictionarySource PRIMARY KEY (id)
);

CREATE TABLE dictionaryRegion (
    id BIGINT NOT NULL,
    name VARCHAR(255) NOT NULL,
    statusID INTEGER NOT NULL,
    CONSTRAINT pk_dictionaryRegion PRIMARY KEY (id)
);

CREATE TABLE salesChannelList (
    partnerID BIGINT NOT NULL,
    salesChannelID VARCHAR(255) NOT NULL,
    CONSTRAINT pk_salesChannelList PRIMARY KEY (id)
);

CREATE TABLE storeExternalCode (
    externalID BIGINT NOT NULL,
    sourceID INTEGER NOT NULL,
    partnerStoreID INTEGER NOT NULL,
    CONSTRAINT pk_storeExternalCode PRIMARY KEY (id)
);

CREATE TABLE partner (
    id BIGINT NOT NULL,
    name VARCHAR(255) NOT NULL,
    partnerCode VARCHAR(255),
    registrDate DATE,
    partnerTypeID INTEGER,
    clientID INTEGER,
    parentPartnerID INTEGER,
    businessRegionID INTEGER,
    mainPartnerID INTEGER,
    managerID INTEGER,
    statusID INTEGER,
    titleID INTEGER,
    CONSTRAINT pk_partner PRIMARY KEY (id)
);

CREATE TABLE partnerRegionList (
    regionID BIGINT NOT NULL,
    partnerID INTEGER NOT NULL,
    CONSTRAINT pk_partnerRegionList PRIMARY KEY (id)
);

CREATE TABLE client (
    id BIGINT NOT NULL,
    fullName VARCHAR(255) NOT NULL,
    shortName VARCHAR(255),
    workName VARCHAR(255),
    internationalName VARCHAR(255),
    legalName VARCHAR(255),
    clientTypeID INTEGER,
    partnerID INTEGER,
    countryID INTEGER,
    mainClientID INTEGER,
    inn VARCHAR(12),
    kpp VARCHAR(9),
    ogrn VARCHAR(15),
    foreignTaxNum VARCHAR(20),
    foreignRegNum VARCHAR(20),
    emailList VARCHAR(255),
    phoneList VARCHAR(255),
    statusID INTEGER,
    CONSTRAINT pk_client PRIMARY KEY (id)
);

CREATE TABLE clientAddressList (
    id BIGINT NOT NULL,
    clientID INTEGER NOT NULL,
    addressTypeID INTEGER NOT NULL,
    address VARCHAR(255) NOT NULL,
    statusID INTEGER NOT NULL,
    CONSTRAINT pk_clientAddressList PRIMARY KEY (id)
);

CREATE TABLE clientContactList (
    id BIGINT NOT NULL,
    clientID INTEGER NOT NULL,
    contactName VARCHAR(255) NOT NULL,
    position VARCHAR(255),
    phone VARCHAR(20),
    email VARCHAR(255),
    statusID INTEGER NOT NULL,
    CONSTRAINT pk_clientContactList PRIMARY KEY (id)
);

CREATE TABLE staff (
    id BIGINT NOT NULL,
    lastName VARCHAR(255) NOT NULL,
    firstName VARCHAR(255),
    middleName VARCHAR(255),
    statusID INTEGER NOT NULL,
    CONSTRAINT pk_staff PRIMARY KEY (id)
);

CREATE TABLE dictionaryClientType (
    id BIGINT NOT NULL,
    name VARCHAR(255) NOT NULL,
    statusID INTEGER NOT NULL,
    CONSTRAINT pk_dictionaryClientType PRIMARY KEY (id)
);

CREATE TABLE dictionaryCountry (
    id BIGINT NOT NULL,
    name VARCHAR(255) NOT NULL,
    statusID INTEGER NOT NULL,
    CONSTRAINT pk_dictionaryCountry PRIMARY KEY (id)
);

CREATE TABLE dictionaryAddressType (
    id BIGINT NOT NULL,
    name VARCHAR(255) NOT NULL,
    statusID INTEGER NOT NULL,
    CONSTRAINT pk_dictionaryAddressType PRIMARY KEY (id)
);

ALTER TABLE storeFacingPlan ADD CONSTRAINT fk_partnerStoreID FOREIGN KEY (partnerStoreID) REFERENCES partnerStore(id);
ALTER TABLE storeFacingPlan ADD CONSTRAINT fk_brandID FOREIGN KEY (brandID) REFERENCES dictionaryBrand(id);

ALTER TABLE storeVisitPlan ADD CONSTRAINT fk_partnerStoreID FOREIGN KEY (partnerStoreID) REFERENCES partnerStore(id);
ALTER TABLE storeVisitPlan ADD CONSTRAINT fk_storeVisitID FOREIGN KEY (storeVisitID) REFERENCES storeVisit(id);

ALTER TABLE storePromoCalendar ADD CONSTRAINT fk_partnerStoreID FOREIGN KEY (partnerStoreID) REFERENCES partnerStore(id);

ALTER TABLE storeVisit ADD CONSTRAINT fk_partnerStoreID FOREIGN KEY (partnerStoreID) REFERENCES partnerStore(id);

ALTER TABLE offTake ADD CONSTRAINT fk_chainUnitID FOREIGN KEY (chainUnitID) REFERENCES partnerStore(id);
ALTER TABLE offTake ADD CONSTRAINT fk_skuID FOREIGN KEY (skuID) REFERENCES dictionarySku(id);

ALTER TABLE partnerStore ADD CONSTRAINT fk_partnerID FOREIGN KEY (partnerID) REFERENCES partner(id);
ALTER TABLE partnerStore ADD CONSTRAINT fk_regionID FOREIGN KEY (regionID) REFERENCES dictionaryRegion(id);
ALTER TABLE partnerStore ADD CONSTRAINT fk_regionID FOREIGN KEY (regionID) REFERENCES diсtionaryStoreFormat(id);
ALTER TABLE partnerStore ADD CONSTRAINT fk_storeTitelID FOREIGN KEY (storeTitelID) REFERENCES storeTitle(id);

ALTER TABLE storeExternalCode ADD CONSTRAINT fk_sourceID FOREIGN KEY (sourceID) REFERENCES dictionarySource(id);
ALTER TABLE storeExternalCode ADD CONSTRAINT fk_partnerStoreID FOREIGN KEY (partnerStoreID) REFERENCES partnerStore(id);

ALTER TABLE partnerRegionList ADD CONSTRAINT fk_regionID FOREIGN KEY (regionID) REFERENCES dictionaryRegion(id);
ALTER TABLE partnerRegionList ADD CONSTRAINT fk_partnerID FOREIGN KEY (partnerID) REFERENCES partner(id);

ALTER TABLE client ADD CONSTRAINT fk_clientTypeID FOREIGN KEY (clientTypeID) REFERENCES dictionaryClientType(id);
ALTER TABLE client ADD CONSTRAINT fk_countryID FOREIGN KEY (countryID) REFERENCES dictionaryCountry(id);

ALTER TABLE clientContactList ADD CONSTRAINT fk_clientID FOREIGN KEY (clientID) REFERENCES client(id);

ALTER TABLE clientAddressList ADD CONSTRAINT fk_clientID FOREIGN KEY (clientID) REFERENCES client(id);
ALTER TABLE clientAddressList ADD CONSTRAINT fk_addressTypeID FOREIGN KEY (addressTypeID) REFERENCES dictionaryAddressType(id);

ALTER TABLE partner ADD CONSTRAINT fk_parentPartnerID FOREIGN KEY (parentPartnerID) REFERENCES dictionaryPartnerType(id);
ALTER TABLE partner ADD CONSTRAINT fk_businessRegionID FOREIGN KEY (businessRegionID) REFERENCES dictionaryBusinessRegion(id);

ALTER TABLE salesChannelList ADD CONSTRAINT fk_partnerID FOREIGN KEY (partnerID) REFERENCES partner(id);
ALTER TABLE salesChannelList ADD CONSTRAINT fk_salesChannelID FOREIGN KEY (salesChannelID) REFERENCES dictionarySalesChannel(id);

ALTER TABLE deliveryZoneList ADD CONSTRAINT fk_zoneID FOREIGN KEY (zoneID) REFERENCES DictionarydeliveryZone(id);

ALTER TABLE table_name ADD CONSTRAINT constraint_name FOREIGN KEY (column_name) REFERENCES referenced_table (referenced_column);


















