# iliPrettyPrint

## todo
- tests
- 

## download

- https://github.com/edigonzales/iliPrettyPrint/releases
- https://s01.oss.sonatype.org/content/repositories/releases/io/github/sogis/iliprettyprint/


## web service

```
curl -v -X POST -F "file=@app/src/test/data/SO_ARP_SEin_Konfiguration_20250115.ili" http://localhost:8080/api/prettyprint 
```

```
curl -v -X POST -F "file=@app/src/test/data/SO_ARP_SEin_Konfiguration_20250115.ili" http://localhost:8080/api/uml 
```

```
curl -v -X POST -F "file=@app/src/test/data/SO_ARP_SEin_Konfiguration_20250115.ili" -F "vendor=PLANTUML" http://localhost:8080/api/uml
curl -v -X POST -F "file=@app/src/test/data/SO_ARP_SEin_Konfiguration_20250115.ili" -F "vendor=MERMAID" http://localhost:8080/api/uml
```

## links

- Sonatype staging repos: https://s01.oss.sonatype.org/#stagingRepositories