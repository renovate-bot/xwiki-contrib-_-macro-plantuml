/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.xwiki.contrib.plantuml.internal.store;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.rendering.macro.MacroExecutionException;
import org.xwiki.resource.ResourceReferenceSerializer;
import org.xwiki.resource.SerializeResourceReferenceException;
import org.xwiki.resource.UnsupportedResourceReferenceException;
import org.xwiki.resource.temporary.TemporaryResourceReference;
import org.xwiki.resource.temporary.TemporaryResourceStore;
import org.xwiki.url.ExtendedURL;

/**
 * Save generated images to a temporary storage location.
 *
 * @version $Id$
 * @since 2.0
 */
@Component
@Named("tmp")
@Singleton
public class TemporaryImageWriter implements ImageWriter
{
    /**
     * The module id used when creating temporary files. This is the module used by the temporary resource handler to
     * retrieve the temporary image file.
     */
    private static final String MODULE_ID = "plantuml";

    /**
     * Used to compute the URL to the temporary stored image generated by the macro.
     */
    @Inject
    private DocumentAccessBridge documentAccessBridge;

    @Inject
    private TemporaryResourceStore temporaryResourceStore;

    // TODO: Once this extension starts depending on XWiki 14.7+, change this to:
    //   @Inject
    //   private ResourceReferenceSerializer<ResourceReference, ExtendedURL> resourceReferenceSerializer;
    // As it is right now, this is hardcoding the URL scheme and would prevent any other Temporary URL scheme to be
    // used.
    @Inject
    @Named("standard/tmp")
    private ResourceReferenceSerializer<TemporaryResourceReference, ExtendedURL> temporaryResourceSerializer;

    @Override
    public OutputStream getOutputStream(String imageId) throws MacroExecutionException
    {
        OutputStream result;
        File imageFile = getStorageLocation(imageId);
        // Make sure that the directory exist
        imageFile.getParentFile().mkdirs();
        try {
            result = new FileOutputStream(imageFile);
        } catch (IOException e) {
            throw new MacroExecutionException(
                String.format("Failed to create the PlantUML image file for image id [%s]", imageId), e);
        }
        return result;
    }

    /**
     * Compute the location where to store the generated image.
     *
     * @param imageId the image id that we use to generate a storage location
     * @return the location where to store the generated image
     * @throws MacroExecutionException if an error happened when computing the location
     */
    protected File getStorageLocation(String imageId) throws MacroExecutionException
    {
        TemporaryResourceReference resourceReference = getTemporaryResourceReference(imageId);
        try {
            return this.temporaryResourceStore.getTemporaryFile(resourceReference);
        } catch (IOException e) {
            throw new MacroExecutionException(String.format("Failed to compute PlantUML image location for [%s]",
                resourceReference), e);
        }
    }

    @Override
    public ExtendedURL getURL(String imageId) throws MacroExecutionException
    {
        TemporaryResourceReference resourceReference = getTemporaryResourceReference(imageId);
        try {
            return this.temporaryResourceSerializer.serialize(resourceReference);
        } catch (SerializeResourceReferenceException | UnsupportedResourceReferenceException e) {
            throw new MacroExecutionException(String.format("Failed to compute PlantUML image URL for [%s]",
                resourceReference), e);
        }
    }

    private TemporaryResourceReference getTemporaryResourceReference(String imageId)
    {
        return new TemporaryResourceReference(MODULE_ID, imageId,
            this.documentAccessBridge.getCurrentDocumentReference());
    }
}
