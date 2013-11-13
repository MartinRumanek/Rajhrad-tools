package cz.mzk.rajhrad.tools;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;

import java.io.File;
import java.util.Properties;

/**
 * @author: Martin Rumanek
 * @version: 11/13/13
 */
public class Configuration {

    private PropertiesConfiguration configuration;

    public Configuration() {
        try {
            configuration = new PropertiesConfiguration("rajhrad.properties");
        } catch (ConfigurationException e) {
            e.printStackTrace();
        }

    }

    public String getSshUserMarcExport() {
        return configuration.getString("marcExportUser");
    }

    public String getSshPasswordMarcExport() {
        return configuration.getString("marcExportPassword");
    }

    public String getSshHostMarcExport() {
        return configuration.getString("marcExportHost");
    }

    public String getPathMarcExport() {
        return configuration.getString("marcExportPath", "/work/aleph/data/aktualizace_zaznamu/mzk03.m21");
    }

    public String getSshUserWorkspace() {
        return configuration.getString("workspaceUser");
    }

    public String getSshHostWorkspace() {
        return configuration.getString("workspaceHost");
    }

    public String getSshPasswordWorkspace() {
        return configuration.getString("workspacePassword");
    }

}
