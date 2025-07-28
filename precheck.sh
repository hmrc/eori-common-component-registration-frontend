#!/bin/bash
sbt clean scalafmt test:scalafmt coverage it:test test coverageReport