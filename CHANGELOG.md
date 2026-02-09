# Changelog

All notable changes to this project will be documented in this file. The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/), and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

Note: Look to the bottom of the change log for a template of fields

## [1.0.109] - 2026-02-04
### changed
- Fix delete dynamic creation args

## [1.0.108] - 2026-02-04
### Changed
- email settings changed

## [1.0.107] - 2026-02-04
### Changed
- Handle extracted json data pdf merge if doesnt obey given schema

## [1.0.106] - 2026-02-04
### Changed
- Fixed upload status codes for corrupted file

## [1.0.105] - 2026-02-04
### Updated
- Updated api to update the multiple conditions status

## [1.0.104] - 2026-02-04
### Added
- added itp email support
- pdf merge - added page count

## [1.0.103] - 2026-02-04
### changed
- Added the Loan Origination Fee and changed COC Date to dateOfLoanEstimateRedisclosure
- Changed TokenService to LoanContextProvider
- Fix email sending template engine process with email
- Handle unassigned attachments on remapping flow

## [1.0.102] - 2026-02-03
### Added
- Added API for Remap_doc_sync

## [1.0.101] - 2026-02-03
### changed
- Removed already processed attachments

## [1.0.100] - 2026-02-03
### changed
- Added logs in poller approach

## [1.0.99] - 2026-02-03
### changed
- Migration file name change

## [1.0.98] - 2026-02-03
### changed
- email directory chnages and reteries count increase
- princeton indexing flow in poller approach

## [1.0.97] - 2026-02-03
### changed
- Fix email sending template engine process

## [1.0.96] - 2026-02-02
### Updated
- Fix email sending when there are no summnary rows 

- ## [1.0.95] - 2026-02-02
### Updated
- Fix more edge cases

- ## [1.0.94] - 2026-02-02
### Updated
- Fix more edge cases

## [1.0.93] - 2026-02-02
### Updated
- Updated auth header in get document api 

## [1.0.92] - 2026-02-02
### Added
- Added file review auto email condition check

## [1.0.91] - 2026-02-02
### Added
- Add fixes for purchase advice and correct multi-document glow

## [1.0.90] - 2026-02-02
### Added
- Added princeton auto email notification for loan approval, suspend, file review

## [1.0.89] - 2026-01-31
### Changed
- Removed document move logic and changes in princeton indexing

## [1.0.88] - 2026-01-29
### Added
- Added email and retry support for uploading documents

## [1.0.87] - 2026-01-27
### Added
- Add support for alerting on Purchase Advices on success also

## [1.0.86] - 2026-01-27
### changed
- princeton automation webhook flow name enum changes
- added loan summary prompt

## [1.0.85] - 2026-01-27
### changed
- Bring in Dottie changes 

## [1.0.84] - 2026-01-26
### changed
- Add the safe navigation for while document not found[DeepHaven]

## [1.0.83] - 2026-01-25
### Added
- Added new setting for folder name and docType mapping for packaging [DeepHaven]

## [1.0.82] - 2026-01-24
### Added
- f the PA extraction using extractionHints

## [1.0.81] - 2026-01-24
### Added
- Changing the PA extraction using extractionHints

## [1.0.80] - 2026-01-23
### Changed
- fix get condition document api issue

## [1.0.79] - 2026-01-22
### Added
- Api to remove attachment and unassign the condition based assigned document

## [1.0.78] - 2026-01-22
### Changed
- Resurrect SINGLE_PASS strategy 
- Refine prompt for single pass
### Added
- Add API for correctly saving the encoded propt

## [1.0.77] - 2026-01-21
### Changed
- Handled image based extraction bugs
- skipped disclosures document in signed disclosure flow

## [1.0.76] - 2026-01-22
### Changed
- Refine the single-pass strategy to include the heuristics  

## [1.0.75] - 2026-01-21
### Changed
- Revise the classification prompt
 
## [1.0.74] - 2026-01-21
### Changed
- Fix conflicts in classification prompt 

## [1.0.73] - 2026-01-21
### Changed
- Add less than condition for the other case too

## [1.0.72] - 2026-01-21
### Changed
- Add configuration for full page dump on pagination

## [1.0.71] - 2026-01-21
### Changed
- return upload attachment response

## [1.0.70] - 2026-01-21
### Changed
- Retain collected json file in regression 

## [1.0.69] - 2026-01-21
### Changed
- Sleep before getting the status

## [1.0.68] - 2026-01-21
### Changed
- Fix recursive issue in tenant settings

## [1.0.67] - 2026-01-21
### Changed
- Changed from content understanding to image based llm extraction
- Ignored source document

## [1.0.66] - 2026-01-22
### Changed
- Approach for accessing variables in regression scripts 

## [1.0.65] - 2026-01-21
### Added
- Add api for saving system settings

## [1.0.64] - 2026-01-21
### Added
- Added api to upload attachment for dottie conditions
- 
## [1.0.63] - 2026-01-20
### Changed
- Added debug step for regressions
### Added
- Add api for self testing extraction models.

## [1.0.62] - 2026-01-20
### Changed
- Fix env name

## [1.0.61] - 2026-01-20
### Changed
- Add null check for regression target API 
- Specify test type as an argument
- LLm retries and save the review message on the log.

## [1.0.60] - 2026-01-19
### Added
- Fix bug related to regression runs

## [1.0.59] - 2026-01-16
### Added
- Api to get the byte array of requested attachment

## [1.0.58] - 2026-01-16
### Modified
- Update TaskBusinessFlowName in repository call

## [1.0.57] - 2026-01-16
### Modified
- Updated TaskTriggeringEventType to milestone

## [1.0.56] - 2026-01-14
### Added
- Added Title Rules Floe Name to TaskBusinessFlowName
- Null Pointer check added in splitPdf Delegate post process

## [1.0.55] - 2026-01-14
### Added
- Added Title Rules Floe Name to TaskBusinessFlowName
- Null Pointer check added in splitPdf Delegate post process

## [1.0.54] - 2026-01-13
### Changed
- Return loan lock message in update notes api for dottie toaster handling

## [1.0.53] - 2026-01-13
### Changed
- Change condition type for retrieve condition documents

## [1.0.52] - 2026-01-13
### Changed
- Update Byte Purchase Advice Dto
- Update saveFile locally method to handle image 

## [1.0.51] - 2026-01-09
### Changed
- Update Byte Purchase Advice Dto and prompt to extract baseLoanAmount

## [1.0.50] - 2026-01-09
### Changed
- Fix extraction confidence for non-array fields.

 
## [1.0.49] - 2026-01-09
### Changed
- Extracting the requiredDocs (LE,CD) after classifying Shipping_package
- add the extracted fields in CSV and send mail


## [1.0.48] - 2026-01-09
### Changed
- Handle Pdf merge using loan folder move webhook changes
- send customized header name in webhooks

## [1.0.47] - 2026-01-08
### Changed
- Purchase Advice flow for Chase

## [1.0.46] - 2026-01-07
### Changed
- Use contentMd5 instead of etag for caching

## [1.0.45] - 2026-01-06
### Changed
- refactor suspension logic

## [1.0.44] - 2026-01-06
### Changed
- Api for clearing setting cache

## [1.0.43] - 2026-01-06
### Changed
- add the rule entity inside pagination

## [1.0.42] - 2026-01-06
### Changed
- add the doc_type into rule entity correctly

## [1.0.41] - 2025-01-06
### Changed
- Update to catch parent exception instead of docflow custom exception in sftp token generation

## [1.0.40] - 2025-01-06
### Changed
- Implement strategy for extracting confidences wrt OCR/extraction/both. 
- Implement caching on tenant Settings. 

## [1.0.39] - 2025-01-06
### Changed
- Instead of Polling Supervisor exponentially,supervisor will trigger only when supervisor check completes.

## [1.0.38] - 2026-01-02
### Changed
- Regex based Image Extraction Implementation

## [1.0.37] - 2025-12-30
### Changed
- Fine-tune LLM based classification approach

## [1.0.36] - 2025-12-30
### Changed
- Added PrivateKey to connect SFTP

## [1.0.35] - 2025-12-30
### Changed
- Create a file in local and upload it to blob.instead of storing in database as byte

## [1.0.34] - 2025-12-30
### Changed
- Expose new api to assign documents for conditions

## [1.0.33] - 2025-12-29
### Changed
- Expose new api to get assigned document and attachments for conditions

## [1.0.32] - 2025-12-29
### Changed
- changed getRoutingMetaForEventType to fetch TenantSettingsMeta by flowName

## [1.0.31] - 2025-12-29
### Changed
- BankStatement extraction for embedded accounts

## [1.0.30] - 2025-12-22
### Changed
- Oak Tree SFTP upload changes
- Dynamic creation Args for task_routes. 
- Supervisor suspension interval revert to exponential backoffs
- Fix missingNode handling on MLE extraction

## [1.0.29] - 2025-12-19
### Changed
- Fix llm retries by giving feedback to the LLM process. 

## [1.0.28] - 2025-12-19
### Changed
- included user timestamp (date, time, initials) in the Encompass notes.
- removed the auto-prefix in Dottie(note section), as the system already knows the user.
- loan lock fixes for note section

## [1.0.27] - 2025-12-18

### Changed
- deephaven classification and emporium allowed documents fix
- rebased with stage for prod deployment
- choice field change for srp buydown funds
- choice field change for srp buydown funds int day
- revert API prefix
- condition update

Related work items: #33090

## [1.0.26] - 2025-12-16

### Changed
- Fix regressions litmus APIs to be based on documentId rather than id
- Ruoff better Implementation
- Fix null parsin on operationLocation on a SubmittedJob.
- Expose env variables for shuffle logic on regression runs
- Sync stage from Brimma.

## [1.0.25] - 2025-12-12

### Changed
- Remove verbose flags and increase timeouts 

## [1.0.24] - 2025-12-12

### Changed
- Make tap an ci dependency

## [1.0.23] - 2025-12-12

### Changed
- Fix exec permissions on script

## [1.0.22] - 2025-12-12

### Changed
- Fix exec permissions on script 

## [1.0.21] - 2025-12-12

### Changed
- Fix regression script

## [1.0.20] - 2025-12-12

### Added
- Sync commits from Brimma

## [1.0.19] - 2025-12-12

### Changed
- Variable referencing in startup.sh
 
## [1.0.18] - 2025-12-12

### Added
- PMD (incremental improvement)
- PMD support on build pipeline
### Changed
- Variable referencing in startup.sh

## [1.0.17] - 2025-12-12

### Changed

- Bumped version to 1.0.17 to align with Octopus release numbering
- Fix variable reference for correct substitution in startup.sh


## [1.0.8] - 2025-12-12

### Changed

- Make stub change to package regressions

## [1.0.7] - 2025-12-12

### Changed

- Unignore script directory

## [1.0.6] - 2025-12-12

### Changed

- Fix regression file temp folder creation.

## [1.0.5] - 2025-12-11

### Added

- Added self boot-ability of regression tests

### Changed

- Push regression test folder as a package.

## [1.0.4] - 2025-12-09

### Changed

- Added regression scripts for testing classification

## [1.0.3] - 2025-11-26

### Changed

- Migrated to new CI/CD process

# Template for log entry

## [SemVer] - yyyy-MM-dd

### Added

- Any added features (typically minor version change)

### Fixed

- Any fixed code/content (typically patch version change)

### Changed

- Any changed content (typcially minor version change if content; major version change if public contract)

### Removed

- Any deletetions (typically major version change)
