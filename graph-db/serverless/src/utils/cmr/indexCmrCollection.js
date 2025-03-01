import gremlin from 'gremlin'
import 'array-foreach-async'

import indexCampaign from './indexCampaign'
import indexPlatform from './indexPlatform'
import indexRelatedUrl from './indexRelatedUrl'
import { deleteCmrCollection } from './deleteCmrCollection'

const gremlinStatistics = gremlin.process.statics

/**
 * Given a collection from the CMR, index it into Gremlin
 * @param {JSON} collectionObj collection object from `items` array in cmr response
 * @param {Gremlin Traversal Object} gremlinConnection connection to gremlin server
 * @returns
 */
export const indexCmrCollection = async (collectionObj, gremlinConnection) => {
  const {
    meta: {
      'concept-id': conceptId
    },
    umm: {
      EntryTitle: entryTitle,
      DOI: {
        DOI: doiDescription
      },
      Projects: projects,
      Platforms: platforms,
      RelatedUrls: relatedUrls
    }
  } = collectionObj

  // delete the collection first so that we can clean up its related documentation vertices
  await deleteCmrCollection(conceptId, gremlinConnection)

  let collection = null
  const addVCommand = gremlinConnection.addV('collection')
    .property('title', entryTitle)
    .property('id', conceptId)

  if (doiDescription) {
    addVCommand.property('doi', doiDescription)
  }

  try {
    // Use `fold` and `coalesce` to check existance of vertex, and create one if none exists.
    collection = await gremlinConnection
      .V()
      .hasLabel('collection')
      .has('id', conceptId)
      .fold()
      .coalesce(
        gremlinStatistics.unfold(),
        addVCommand
      )
      .next()
  } catch (error) {
    console.error(`Error indexing collection [${conceptId}]: ${error.message}`)

    return false
  }

  const { value = {} } = collection
  const { id: collectionId } = value

  if (projects && projects.length > 0) {
    await projects.forEachAsync(async (project) => {
      await indexCampaign(project, gremlinConnection, collectionId, conceptId)
    })
  }

  if (platforms && platforms.length > 0) {
    await platforms.forEachAsync(async (platform) => {
      await indexPlatform(platform, gremlinConnection, collectionId, conceptId)
    })
  }

  if (relatedUrls && relatedUrls.length > 0) {
    await relatedUrls.forEachAsync(async (relatedUrl) => {
      await indexRelatedUrl(relatedUrl, gremlinConnection, collectionId, conceptId)
    })
  }

  console.log(`Collection vertex [${collectionId}] indexed for collection [${conceptId}]`)

  return true
}
