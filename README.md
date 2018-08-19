# Jenkins StatsD Client Plugin

(WIP) Plugin to allow instrumentation of Jenkins Pipeline jobs with arbitrary statistics sent to a StatsD server

## Usage

### Configuration
Server address and port configuration in Global System Configuration under "StatsD Client Plugin"

### Counters
```
node {
	statsd 'metric-name' // increment 'metric-name' counter
	statsd prefix: 'some.other.prefix', metric: 'metric-name' // increment 'some.other.prefix.metric-name' counter
}
```

### Block Timing
```
node {
	// sends `execution` milliseconds timer metric after the block finishes
	withStatsDTiming {
		sh './some-long-process.sh'
	}

	// sends `some.prefix.execution` milliseconds timer
	withStatsDTiming('some.prefix') {
		sh './some-long-process.sh'
	}
}
```

## TODO list
 * Allow multiple prefixes and push stats to all of them
 * Expand the simple build step beyond counters
 * Support nested blocks where the prefix is inherited and prepended to children
 * Support cross block timers (e.g. start a timer before a queue wait, and stop it when the next part resumes)
 * More tests
 
 ## License
 
 MIT