# Poisace

## Rationale
Poisauce is a simple webservice to parse Microsoft Excel (XLS) documents using the excellent Apache Poi (http://poi.apache.org/) library and return simple JSON documents.

## Running
`sbt run`

## Interface
`POST /` with the contents of the XLS document as the request body.

## Availability
Poisauce is currently available at http://young-snow-1139.herokuapp.com/