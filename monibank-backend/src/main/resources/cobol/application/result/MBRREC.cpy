       01  MBR-RESULT-RECORD             PIC X(160).

       01  MBR-RESULT-HEADER
           REDEFINES MBR-RESULT-RECORD.
           05  MBR-H-PREFIX               PIC X(3).
           05  MBR-H-SEP-0                PIC X.
           05  MBR-H-TYPE                 PIC X.
           05  MBR-H-SEP-1                PIC X.
           05  MBR-H-OPERATION            PIC X(8).
           05  MBR-H-SEP-2                PIC X.
           05  MBR-H-REQUEST-ID           PIC X(8).
           05  MBR-H-SEP-3                PIC X.
           05  MBR-H-ENTITY-ID            PIC X(13).
           05  MBR-H-SEP-4                PIC X.
           05  MBR-H-STATUS               PIC X.
           05  MBR-H-SEP-5                PIC X.
           05  MBR-H-CODE                 PIC X(20).
           05  FILLER                     PIC X(100).

       01  MBR-RESULT-DATA
           REDEFINES MBR-RESULT-RECORD.
           05  MBR-D-PREFIX               PIC X(3).
           05  MBR-D-SEP-0                PIC X.
           05  MBR-D-TYPE                 PIC X.
           05  MBR-D-SEP-1                PIC X.
           05  MBR-D-ENTITY               PIC X(8).
           05  MBR-D-SEP-2                PIC X.
           05  MBR-D-REQUEST-ID           PIC X(8).
           05  MBR-D-SEP-3                PIC X.
           05  MBR-D-PAYLOAD              PIC X(136).