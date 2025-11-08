package db.documenter;

import java.util.List;

public record DbDocumenterConfig(List<String> schemas,
                                 String databaseHost,
                                 int databasePort,
                                 String databaseName,
                                 boolean useSSL,
                                 String username,
                                 String password) {

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private List<String> schemas;
        private String databaseHost;
        private String databaseName;
        private int databasePort = 5432;
        private boolean useSSL = true;
        private String username;
        private String password;

        public Builder schemas(final List<String> schemas) {
            this.schemas = schemas;
            return this;
        }

        public Builder databaseHost(final String databaseHost) {
            this.databaseHost = databaseHost;
            return this;
        }

        public Builder databasePort(final int databasePort){
            this.databasePort = databasePort;
            return this;
        }

        public Builder databaseName(final String databaseName){
            this.databaseName = databaseName;
            return this;
        }

        public Builder useSSL(final boolean useSSL) {
            this.useSSL = useSSL;
            return this;
        }

        public Builder username(final String username) {
            this.username = username;
            return this;
        }

        public Builder password(final String password) {
            this.password = password;
            return this;
        }

        public DbDocumenterConfig build() {
            return new DbDocumenterConfig(schemas, databaseHost, databasePort, databaseName, useSSL, username, password);
        }

    }
}
