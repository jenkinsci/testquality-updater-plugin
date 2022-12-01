
# TestQuality Jenkins Plug-in

This is [Jenkins](http://jenkins.io) Plug-in for uploading test results to [TestQuality](http://www.testquality.com). So that
your test results can be analyzed, to help you produce better tests with fewer resources.


# Jenkins pipeline example
```groovy
pipeline {
    agent any

    stages {
        stage('junit') {
            steps {
                sh 'mvn test'
                testQuality cycle: 'Plan 2-Project 1', project: 'Project 1', testResults: 'target/surefire-reports/*'
            }
        }
    }
}
```

# Build

    mvn package

# License
 
    MIT