
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
                testQuality milestone: '-1', cycle: '1', project: '1', testResults: 'target/surefire-reports/*'
            }
        }
    }
}
```

# Build

    mvn package

# License
 
    MIT