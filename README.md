# escher-request-sampler

### Instructions

1. Install JMeter <https://jmeter.apache.org/download_jmeter.cgi>
1. Build the project `./gradlew installDist`
1. Move the generated jar files into your JMeter installation's `lib/ext/` directory
1. Open the JMeter GUI and you should see the custom Sampler named `pecc.EscherRequestSampler`!
  * Create a simple JMeter "Test Plan" with these steps:
    1. Right-click "Test Plan" in the left-hand menu
    1. On the menu that appears, navigate through and select "Add" > "Threads (Users)" > "Thread Group"
    1. Right-click the "Thread Group" element that appears in the tree on the left
    1. On the menu that appears, navigate through and select "Add" > "Sampler" > "Java Request"
    1. On the "Java Request" screen that appears, click the dropdown menu
    1. You should see the custom sampler, `pecc.EscherRequestSampler`
    1. Fill in the fields, like scope, key, secret, etc. 
