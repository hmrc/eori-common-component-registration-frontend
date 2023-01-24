#!/bin/bash

sbt clean -mem 2048 scalafmt test:scalafmt coverage test it:test coverageReport
