/*
 * ATLauncher - https://github.com/ATLauncher/ATLauncher
 * Copyright (C) 2013 ATLauncher
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package com.atlauncher.data.loaders.forge;

import java.util.List;
import java.util.Map;

import com.atlauncher.annot.Json;

@Json
public class Processor {
    private String jar;
    private List<String> classpath;
    private List<String> args;
    private Map<String, String> outputs;

    public String getJar() {
        return this.jar;
    }

    public List<String> getClasspath() {
        return this.classpath;
    }

    public List<String> getArgs() {
        return this.args;
    }

    public Map<String, String> getOutputs() {
        return this.outputs;
    }
}
