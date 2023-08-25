CREATE TABLE Caregivers (
    Username varchar(255),
    Salt BINARY(16),
    Hash BINARY(16),
    PRIMARY KEY (Username)
);

CREATE TABLE Availabilities (
    Time date,
    Username varchar(255) REFERENCES Caregivers,
    PRIMARY KEY (Time, Username)
);

CREATE TABLE Vaccines (
    Name varchar(255),
    Doses int,
    PRIMARY KEY (Name)
);

CREATE TABLE Patients (
    Username varchar(255),
    Salt BINARY(16),
    Hash BINARY(16),
    PRIMARY KEY (Username)
);

CREATE TABLE Appointments (
    AppointmentID varchar(6),
    Time date,
    Vaccine varchar(255) NOT NULL REFERENCES Vaccines(Name),
    Patient varchar(255) NOT NULL REFERENCES Patients(Username),
    Caregiver varchar(255) NOT NULL REFERENCES Caregivers(Username),
    PRIMARY KEY (AppointmentID)
);