/*
 * Copyright (C) 2003-2008 eXo Platform SAS.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 */
package org.exoplatform.services.wcm.extensions.deployment;

import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.ObjectParameter;
import org.exoplatform.container.xml.PortalContainerInfo;
import org.exoplatform.services.deployment.DeploymentPlugin;
import org.exoplatform.services.ecm.publication.PublicationService;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.wcm.publication.WCMPublicationService;

import javax.jcr.Node;
import javax.jcr.Session;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

/**
 * Created by The eXo Platform SAS
 * Author : Hoa Pham
 * hoa.pham@exoplatform.com
 * Sep 6, 2008
 */
public class PublicationDeploymentPlugin extends DeploymentPlugin {

  /** The init params. */
  private InitParams initParams;

  /** The repository service. */
  private RepositoryService repositoryService;

  /** The publication service */
  private WCMPublicationService wcmPublicationService;

  /** The publication service */
  private PublicationService publicationService;

  /** The log. */
  private Log log = ExoLogger.getLogger(this.getClass());
  public static final String UPDATE_EVENT = "WCMPublicationService.event.updateState";

  /**
   * Instantiates a new xML deployment plugin.
   *
   * @param initParams the init params
   * @param repositoryService the repository service
   * @param publicationService the publication service
   */
  public PublicationDeploymentPlugin(InitParams initParams,
                                     RepositoryService repositoryService,
                                     PublicationService publicationService,
                                     WCMPublicationService wcmPublicationService) {
    this.initParams = initParams;
    this.repositoryService = repositoryService;
    this.publicationService = publicationService;
    this.wcmPublicationService = wcmPublicationService;
  }

  /* (non-Javadoc)
   * @see org.exoplatform.services.deployment.DeploymentPlugin#deploy(org.exoplatform.services.jcr.ext.common.SessionProvider)
   */
  public void deploy(SessionProvider sessionProvider) throws Exception {
    Iterator iterator = initParams.getObjectParamIterator();
    PublicationDeploymentDescriptor deploymentDescriptor = null;
    try {
      while (iterator.hasNext()) {
        ObjectParameter objectParameter = (ObjectParameter) iterator.next();
        deploymentDescriptor = (PublicationDeploymentDescriptor) objectParameter.getObject();
        List<String> contents = deploymentDescriptor.getContents();
        // sourcePath should looks like : collaboration:/sites
        // content/live/acme

        HashMap<String, String> context_ = new HashMap<String, String>();
        ExoContainer container = ExoContainerContext.getCurrentContainer();
        PortalContainerInfo containerInfo = (PortalContainerInfo) container.getComponentInstanceOfType(PortalContainerInfo.class);
        String containerName = containerInfo.getContainerName();
        context_.put("containerName", containerName);


        for (String sourcePath:contents) {
          try {
            String[] src = sourcePath.split(":");

            if (src.length == 2) {
              ManageableRepository repository = repositoryService.getCurrentRepository();
              Session session = sessionProvider.getSession(src[0], repository);
              Node nodeSrc = session.getRootNode().getNode(src[1].substring(1));

              wcmPublicationService.updateLifecyleOnChangeContent(nodeSrc, "default", "__system", "published");
              nodeSrc.save();

            }
            if (log.isInfoEnabled()) {
              log.info(sourcePath + " has been published.");
            }
          } catch (Exception ex) {
            if (log.isErrorEnabled()) {
              log.error("publication for " + sourcePath + " FAILED at "
                          + new Date().toString() + "\n",
                      ex);
            }
          }
        }


      }
    } catch (Exception ex) {
      if (log.isErrorEnabled()) {
        log.error("publication plugin FAILED at "
                    + new Date().toString() + "\n",
                ex);
      }
      throw ex;
    }
  }
}