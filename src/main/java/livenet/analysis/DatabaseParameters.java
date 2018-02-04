package livenet.analysis;

/*
 * Copyright (c) 2000-2018 Robert Biuk-Aghai
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you
 * may not use this file except in compliance with the License.  You
 * may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */

public class DatabaseParameters implements java.io.Serializable
{
    String server;
    int port;
    String database;
    String user;
    String password;


    public DatabaseParameters(String server, int port, String user,
			      String password)
    {
	this(server, port, null, user, password);
    }


    public DatabaseParameters(String server, int port, String database,
			      String user, String password)
    {
	this.server = server;
	this.port = port;
	this.database = database;
	this.user = user;
	this.password = password;
    }
}
