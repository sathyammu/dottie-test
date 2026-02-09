INSERT INTO extraction_hubs (document_type,analyzer_id,hub_id,field_schema, strategy) VALUES
	 ('bank_statement','bank_statement','none','
	 {
       "fields": {
         "bankName": {
           "type": "string",
           "method": "extract",
           "description": "Bank / Financial Institution Name"
         },
         "accountNo": {
           "type": "string",
           "method": "extract",
           "description": "Customer Account Number"
         },
         "endingBalance": {
           "type": "currency",
           "method": "extract",
           "description": "Ending Balance"
         },
         "customerAddress": {
           "type": "string",
           "method": "extract",
           "description": "Bank Customer / Account Holder Address - Address, City, State, Zip"
         },
         "beginningBalance": {
           "type": "currency",
           "method": "extract",
           "description": "Beginning Balance"
         },
         "accountTransactions": {
           "type": "array",
           "items": {
             "type": "object",
             "method": "extract",
             "properties": {
               "rowId": {
                 "type": "number",
                 "method": "extract",
                 "isInternal": true,
                 "description": "row number along with the table number in the format T:<n>,R:<n> ,where T is for Table and Row is for row"
               },
               "amount": {
                 "type": "currency",
                 "method": "extract",
                 "description": "Transaction Amount(only credit/debit transactions). Dont add any additional formatting for the credit debit like +/- or `()` since the transactionType is tracked on another field"
               },
               "description": {
                 "type": "string",
                 "method": "extract",
                 "description": "Description of the transaction"
               },
               "transactionDate": {
                 "type": "date",
                 "method": "extract",
                 "description": "Should be within the statementStartDate and statementEndDate"
               },
               "transactionType": {
                 "type": "string",
                 "method": "extract",
                 "description": "credit/debit"
               }
             },
             "description": "Do not return rows with Empty values for amount"
           },
           "method": "generate",
           "description": "Account Transactions"
         },
         "firstAccountHolderName": {
           "type": "string",
           "method": "extract",
           "description": "Bank Customer / Account Holder Name - First Customer"
         },
         "statementPeriodEndDate": {
           "type": "date",
           "method": "extract",
           "description": "Bank Statement Period - End Date"
         },
         "secondAccountHolderName": {
           "type": "string",
           "method": "extract",
           "description": "Bank Customer / Account Holder - Second Account Holder(Joined Account). Be careful in not returning both the first account holder and the second. You''ve already parsed the primaryAccountHolder, and we require just the secondaryAccountHolder in this case"
         },
         "statementPeriodStartDate": {
           "type": "date",
           "method": "extract",
           "description": "Bank Statement Period - Start Date"
         }
       }
     }', 'MULTI_LEVEL_EXTRACT');
