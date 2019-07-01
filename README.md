
= Instructions: 

1. Install [Anaconda](https://docs.anaconda.com/anaconda/install/)
2. Download [deepwalk.yml](https://github.com/syedfahadsultan/DeepwalkPluginForNeo/blob/3.5/deepwalk.yml)
3. Call `conda env create -f deepwalk.yml`
4. Call `conda activate deepwalk`
5. Add to Neo settings: i) `CONDA=<path to where conda is installed>` ii) `dbms.security.functions.whitelist=example.*` iii) `dbms.security.procedures.whitelist=example.*` iv) `dbms.security.procedures.unrestricted=example.*`
6. Cypher query: `CALL example.deepWalk();`