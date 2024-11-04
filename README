# Railflow Jenkins Plugin

Jenkins is one of the most popular CI applications and is used by thousands of companies all over the world. Jenkins is used to define the application build and test processes. QA teams also use Jenkins to run/schedule functional tests across a variety of tools and frameworks. However, the results of these tests can only be viewed in Jenkins, and it is not really possible to aggregate results across multiple jobs, retain long history, and delegate test failures to your team.  

By using the Railflow Jenkins Plugin, you can easily integrate Jenkins testing jobs with TestRail and automatically export all testing reports to TestRail. Aggregating result from all your Jenkins jobs into TestRail allows teams to look at test trends, auto-assign failures via Railflow automation, create link between Jenkins and TestRail, and much more. 

## Jenkins Plugin

Railflow Jenkins plugin is an open source product from version 2.9 onwards. As such, it is now available in the Jenkins plugin directory. However you still need a valid license is required to use the Railflow CLI tools.

### Prerequisites

- Jenkins version 2.204.1 or newer

### Installation

1. Navigate to the `Manage Jenkins | Plugins | Available plugins` page of the Jenkins UI to see the list of available plugins.
2. Select the `Railflow for TestRail` plugin and click `Install`.

### Configuration

To configure plugin, navigate to `Manage Jenkins | Configure System` page of the Jenkins UI and scroll down to the `Railflow Global Settings` section:

![Jenkins plugin config](docs/jenkins-plugin-config.png)

### Licensing 

Railflow provides two license activation models. If your network allows outbound call to our api endpoint: `https://api.railflow.io`, then option 1: `License Key (online activation) ` will work. If you are unable to reach our api endpoint, pick option 2: `License File (offline activation)`.

1. **License Key** (online activation): Select `License Key` option and copy/paste the license key from your email and click `Activate License`

![Jenkins plugin online activation](docs/online-activation.png)

2. **License File** (offline activation): If your Jenkins instance does not have outbound internet access, you may use this option to upload the license activation from your email. The license file has a .skm extension.

![Jenkins plugin offline activation](docs/offline-license-activation.png)

### TestRail Configuration

Railflow plugin configuration section allows you to centrally defined one or multiple TestRail servers. Once configured, they can be easily referenced in the Jenkins jobs. 

![Jenkins plugin config](docs/plugin04.png)

### Freestyle Job Configuration

If you are using Jenkins UI based (freestyle job) jobs to run some test frameworks/tools, you can use the Railflow plugin's UI controls to easily integrate with TestRail by following these steps.

Add and configure the Jenkins job's post-build action: `Railflow Plugin: TestRail Test Results Processor`. Railflow's post-build action allows you to specify the test framework, test results location, and various other TestRail configurations.

![Jenkins plugin](docs/plugin-job-1.png)

For additional TestRail export functionality, check out the `Advanced` section.

![Jenkins plugin](docs/plugin-job-3.png)

### Jenkinsfile Job Configuration

If you are using a Jenkinsfile to run some test frameworks/tools, you can access the same plugin functionality within the Jenkinsfile and easily integrate with TestRail by following these steps

```jsx title="Jenkins Pipeline Example running JUnit test project"
pipeline {
    agent {
        label "linux"
    }
    stages {
       stage('Build') {
          steps {
             echo '********BUILD STEP**********'
             git branch: 'master', credentialsId: '*****', url: 'https://...junit-demo.git'
             sh 'mvn test'
          }
       }
    }
    post {
     always {
     echo '********UPLOAD RESULT INTO TESTRAIL**********'

    railflow(
        testRailServerName: 'TestRail',
        testRailProjectName: 'Railflow Demo',
        failIfUploadFailed: true, 
        overriddenUserName: 'userName', 
        overriddenPassword: 'password',
        jobConfigurations: [[configurationNames: '''OS/Linux
            Browser/Chrome''', 
        milestonePath: 'M1/M2', 
        resultFilePattern: '**/surefire-reports/*.xml',
        uploadMode: 'CREATE_NO_UPDATE',
        searchMode: 'Path',
        testCaseCustomFields: '''Required text field=Hello from Jenkins
            estimate=30s''', 
        testCasePath: 'Railflow/Website', 
        testCasePriority: 'High', 
        testCaseType: 'Automated', 
        testPlanName: 'Test plan', 
        testReportFormat: 'JUNIT', 
        testResultCustomFields: '''Custom field=Results from Jenkins
            version=2.0''', 
        testRunName: '${JOB_NAME}-${BUILD_NUMBER}'
        smartTestFailureAssignment: 'user1@abc.com, user2@abc.com', 
        testCaseTemplate: 'template1', 
        caseSearchField: 'case search field name', 
        tagsFieldName: 'tags field name',
        uploadMode: 'CREATE_NO_UPDATE', 
        disableGrouping: true, 
        closeRun: true, 
        closePlan: true, 
        fullCaseNamesAllowed: true ]])
      }
    }
}
```

#### Railflow param definitions

| Key                                | Required | Description 
| -----------------------------------|----------|----------------
| testRailServerName                 | Yes      |  One of the server names configured in "Global settings configuration"
| testRailProjectName                | Yes      |  The name of a project in TestRail to which results should be exported
| failIfUploadFailed                 | No       |  If true, the build will be marked as failed if for any reason the plugin could not upload the results. This could be due to Railflow issues, TestRail server issues, network issues, etc.
| overriddenUserName                 | No       |  If specified, it overrides TestRail user name defined in Global Configuration
| overriddenPassword                 | No       |  If specified, it overrides TestRail password defined in Global Configuration
| jobConfigurations                  | Yes      |  This can contain job configuration parameters defined below

##### Job configuration parameters

| Key                        | Required | Description
|----------------------------|----------|----------------
| resultFilePattern          | Yes      |  The file path to the test report file(s) generated during the build. Ant-style patterns such as **\*\*/surefire-reports/\*.xml** can be used.<br/>E.g. use **target/surefire-reports/*.xml** to capture all XML files in **target/surefire-reports** directory.
| searchMode                 | Yes      |  Specifies the test case lookup algorithm. <br/> **`Name:`** search for test case matching the name within the entire test suite. If test case found, update the test case. If test case not found, create a new test case within specified Test Case Path <br/> **`Path`**: search for test case matching the name within the specified Test Case Path. If test case found, update the test case. If test case not found, create a new test case within specified Test Case Path
| testCasePath               | No      |  Path to where Railflow should upload test case definitions, must contain suite name in the beginning (for single-suite project, suite name is always 'Master'), e.g. Master/Section1/Section2
| testPlanName               | No      |  Name of a test plan in TestRail to which test results will be added
| testRunName                | No      |  Name of a test run in TestRail to which test results will be added
| milestonePath              | No      |  Path to a milestone in TestRail to which test run/plan will be added.<br/>E.g. Milestone1/Milestone2
| smartTestFailureAssignment | No      |  A comma separated list of user emails for smart failure assignment. Each failed result will be assigned to a person from this list in a round robin fashion<br/>
| testCaseType               | No      |  Name of a case type in TestRail, e.g. `Automated`
| testCasePriority           | No      |  Name of a case priority in TestRail, e.g. `High`
| testCaseTemplate           | No      |  Name of a TestRail template. If it is blank, `Test Case (Steps)` will be used
| testCaseCustomFields       | No      |  Values for case fields in TestRail can be specified in this field. The format is [TestRail field label]=[value] and each field name\\value pair should start with the new line.<br/>E.g.:<br/>Custom Field One=foo<br/>Custom Field Two=bar
| testResultCustomFields     | No      |  Values for result fields in TestRail can be specified in this field. The format is [TestRail field label]=[value] and each field name\\value pair should start with the new line.<br/>E.g.:<br/>Custom Result Field One=foo<br/>Custom Result Field Two=bar
| configurationNames         | No      | A list of configuration names in TestRail. The format is [Config Group Name]/[Config Name]. Each entry must start with the new line.<br/>E.g.:<br/>Operating Systems/Linux<br/>Browsers/Chrome
| caseSearchField            | No      |  The name of the case field in TestRail which will be using for searching for existing test cases instead of test case title
| tagsFieldName              | No      | The name of a test case field which will be holding tags from the report file if any. E.g. `Cucumber Tags`
| uploadMode                 | No      |  <div> <b>Test case upload mode:</b> <ul> <li><b>Create new test cases and do not overwrite existing ones:</b><br/> If test case not found, create a new test case within specified Test Case Path.<br/> If test case found, do not update the test case.<br/> Value for pipeline: CREATE_NO_UPDATE </li> <li><b>Create new cases and overwrite existing ones:</b><br/> If test case not found, create a new test case within specified Test Case Path.<br/> If test case found, update the test case.<br/> Value for pipeline: CREATE_UPDATE </li> <li> <b>Do not create new cases and overwrite existing ones:</b><br/> If test case not found, do not create a new test case and the corresponding test result will not be uploaded into TestRail.<br/> If test case found, update the test case.<br/> Value for pipeline: NO_CREATE_UPDATE </li> <li> <b>Do not create new cases and do not overwrite existing ones:</b><br/> If test case not found, do not create a new test case and the corresponding test result will not be uploaded into TestRail.<br/> If test case found, do not update the test case.<br/> Value for pipeline: NO_CREATE_NO_UPDATE </li> </ul> </div>
| disableGrouping            | No      |  If true, Railflow will ignore structure in report files and upload all test cases into one Section, defined by the Test Path parameter.
| closeRun                   | No      |  If true, Railflow will close the test run in TestRail and archive its tests and results
| closePlan                  | No      |  If true, Railflow will close the test plan in TestRail and archive its tests and results
| fullCaseNamesAllowed       | No      |  If true, Railflow will use fully qualified test names from the report files for test names in TestRail

### Post-Build Action Reference

Both the Jenkinsfile and Freestyle job depend on the `Railflow Plugin: TestRail Test Results Processor` post-build action to process test results and post them to TestRail. This action provides users a host of options to integrate Jenkins with TestRail in a variety of ways and across any testing tool and framework. The reference below describes all the options and their usage.

You can add multiple Raillflow post-build actions using `Add More` button. This may be needed if you have multiple reports to process and want to map each report to different places in TestRail.

| Field Name                        | Required | Description                                                                                                                                                                                                                                                                                                                                                                                       |
|-----------------------------------|----------|-------------|
| TestRail Server 	                 | Yes	| Select one of the server names configured in "Global settings configuration"	|
| TestRail Project                  | Yes   | The name of a project in TestRail to which results should be exported			|
| Fail build if upload unsuccessful | N/A      | If checked, the build will be marked as failed if for any reason the plugin could not upload the results. This could be due to Railflow issues, TestRail server issues, network issues, etc.	|
| Override TestRail Credentials	    | No	| If specified, it overrides TestRail user credentials defined in Global Configuration	|
| Results File Pattern              | Yes      | The file path to the test report file(s) generated during the build. Ant-style patterns such as **\*\*/surefire-reports/\*.xml** can be used.<br/>E.g. use **target/surefire-reports/*.xml** to capture all XML files in **target/surefire-reports** directory.	|
| Report Format                     | Yes      | Results file format	|
| Search Mode                       | Yes      | Specifies the test case lookup algorithm. <br/> **`Name:`** search for test case matching the name within the entire test suite. If test case found, update the test case. If test case not found, create a new test case within specified Test Case Path <br/> **`Path`**: search for test case matching the name within the specified Test Case Path. If test case found, update the test case. If test case not found, create a new test case within specified Test Case Path	|
| Test Case Path				                | Yes | Path to where Railflow should upload test case definitions, must contain suite name in the beginning (for single-suite project, suite name is always 'Master'), e.g. Master/Section1/Section2 |
| Test Plan Name                    | No       | Name of a test plan in TestRail to which test results will be added |
| Test Run Name                     | No       | Name of a test run in TestRail to which test results will be added |
| Milestone Path                    | No       | Path to a milestone in TestRail to which test run/plan will be added.<br/>E.g. Milestone1/Milestone2 |
| Smart Test Failure Assignment     |	No	| A comma separated list of user emails for smart failure assignment. Each failed result will be assigned to a person from this list in a round robin fashion<br/>|
| Log Level                         | No | Logging level for the plugin log file (.railflow.log) |


| Advanced Field Name       | Description                                                                                                                                                                                                                                                                                                                                                                                  |
|---------------------------|---------------------------------------------------------------------------------------|
| Test Case Type            | Name of a case type in TestRail, e.g. `Automated` |
| Test Case Priority        | Name of a case priority in TestRail, e.g. `High` |
| Test Case Template        | Name of a TestRail template. If it is blank, `Test Case (Steps)` will be used. |
| Test Case Custom Fields   |  Values for case fields in TestRail can be specified in this field. The format is [TestRail field label]=[value] and each field name\\value pair should start with the new line.<br/>E.g.:<br/>Custom Field One=foo<br/>Custom Field Two=bar |
| Test Result Custom Fields | Values for result fields in TestRail can be specified in this field. The format is [TestRail field label]=[value] and each field name\\value pair should start with the new line.<br/>E.g.:<br/>Custom Result Field One=foo<br/>Custom Result Field Two=bar |
| Configuration Names       | A list of configuration names in TestRail. The format is [Config Group Name]/[Config Name]. Each entry must start with the new line.<br/>E.g.:<br/>Operating Systems/Linux<br/>Browsers/Chrome|
| Case Search Field         | The name of the case field in TestRail which will be using for searching for existing test cases instead of test case title |
| Tags Field Name           | The name of a test case field which will be holding tags from the report file if any. E.g. `Cucumber Tags`
| Upload Mode               | <div> <b>Test case upload mode:</b> <ul> <li><b>Create new test cases and do not overwrite existing ones:</b><br/> If test case not found, create a new test case within specified Test Case Path.<br/> If test case found, do not update the test case.<br/> Value for pipeline: CREATE_NO_UPDATE </li> <li><b>Create new cases and overwrite existing ones:</b><br/> If test case not found, create a new test case within specified Test Case Path.<br/> If test case found, update the test case.<br/> Value for pipeline: CREATE_UPDATE </li> <li> <b>Do not create new cases and overwrite existing ones:</b><br/> If test case not found, do not create a new test case and the corresponding test result will not be uploaded into TestRail.<br/> If test case found, update the test case.<br/> Value for pipeline: NO_CREATE_UPDATE </li> <li> <b>Do not create new cases and do not overwrite existing ones:</b><br/> If test case not found, do not create a new test case and the corresponding test result will not be uploaded into TestRail.<br/> If test case found, do not update the test case.<br/> Value for pipeline: NO_CREATE_NO_UPDATE </li> </ul> </div> |
| Disable Grouping          | If checked, Railflow will ignore structure in report files and upload all test cases into one Section, defined by the Test Path parameter. |
| Close Run	             | If checked, Railflow will close the test run in TestRail and archive its tests and results |
| Close Plan		         | If checked, Railflow will close the test plan in TestRail and archive its tests and results |
| Fully Qualified Test Name | If checked, Railflow will use fully qualified test names from the report files for test names in TestRail |
