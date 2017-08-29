/**
 * Copyright (C) 2015-2016 DANS - Data Archiving and Networked Services (info@dans.knaw.nl)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package nl.knaw.dans.easy.solr

import org.scalamock.scalatest.MockFactory
import org.scalatest._
import org.slf4j.Logger
import scala.xml._

class SolrDocumentGeneratorSpec extends FlatSpec
    with Matchers
    with Inside
    with MockFactory
    with OneInstancePerTest {
  /*
   * Mocking and helper functions.
   */
  val fedora = mock[FedoraProvider]
  val log = mock[Logger]

  private def expectEmptyXmlByDefault = {
    expectDc(<dc/>)
    expectEmd(<easymetadata/>)
    expectAmd(<amd />)
    expectPrsl(<permissionSequenceList />)
    expectRelsExt(<RDF />)
  }

  private def expectDc(xml: Elem) = fedora.getDc _ expects * anyNumberOfTimes() returning xml.toString
  private def expectEmd(xml: Elem) = fedora.getEmd _ expects * anyNumberOfTimes() returning xml.toString
  private def expectAmd(xml: Elem) = fedora.getAmd _ expects * anyNumberOfTimes() returning xml.toString
  private def expectPrsl(xml: Elem) = fedora.getPrsql _ expects * anyNumberOfTimes() returning xml.toString
  private def expectRelsExt(xml: Elem) = fedora.getRelsExt _ expects * anyNumberOfTimes() returning xml.toString
  private def expectWarningLogged() = (log.warn(_: String)) expects * atLeastOnce()

  private def getSolrDocFieldValues(docRoot: Elem, field: String): Seq[String] =
    (docRoot \\ "doc" \ "field").filter(f => (f \ "@name").text == field).map(_.text)

  /*
   * Tests
   */

  "minimal EMD" should "result in only standard fields" in {
    expectEmptyXmlByDefault
    val docRoot = new SolrDocumentGenerator(fedora, "test-pid:123").toXml

    docRoot.label should be("doc")
    val fields = (docRoot \ "field").map(f => (f \ "@name").text -> f.text)
    fields should have length 5
    fields should contain("sid" -> "test-pid:123")
    fields should contain("type" -> "easy-dataset")
    fields should contain("type" -> "dataset")
    fields should contain("repository_id" -> "easy")
    fields should contain("amd_workflow_progress" -> "0")
  }

  "all dc_* fields" should "be extracted from EMD" in {
    expectEmd(
      <easymetadata xmlns:eas="http://easy.dans.knaw.nl/easy/easymetadata/eas/">
      <emd:title>
        <dc:title>title</dc:title>
      </emd:title>
      <emd:description>
        <dc:description>description</dc:description>
      </emd:description>
      <emd:publisher>
        <dc:publisher>publisher</dc:publisher>
      </emd:publisher>
      <emd:subject>
        <dc:subject>subject</dc:subject>
      </emd:subject>
      <emd:type>
        <dc:type eas:scheme="DCMI" eas:schemeId="common.dc.type">Dataset</dc:type>
      </emd:type>
      <emd:format>
        <dc:format>format</dc:format>
      </emd:format>
      <emd:identifier>
        <dc:identifier eas:scheme="DMO_ID">easy-dataset:123</dc:identifier>
      </emd:identifier>
      <emd:source>
        <dc:source>source</dc:source>
      </emd:source>
      <emd:language>
        <dc:language eas:scheme="ISO 639" eas:schemeId="common.dc.language">dut/nld</dc:language>
      </emd:language>
      <emd:rights>
        <dct:accessRights eas:schemeId="archaeology.dcterms.accessrights">OPEN_ACCESS</dct:accessRights>
      </emd:rights>
      <emd:coverage>
        <dct:spatial>spatial</dct:spatial>
      </emd:coverage>
      <emd:date>
        <dct:created>2016-01-01</dct:created>
      </emd:date>
        <emd:relation>
          <dc:relation>relation</dc:relation>
        </emd:relation>
        <emd:creator>
          <eas:creator>
            <eas:organization>creator-org</eas:organization>
          </eas:creator>
        </emd:creator>
        <emd:contributor>
          <eas:contributor>
            <eas:organization>contributor-org</eas:organization>
          </eas:contributor>
        </emd:contributor>
    </easymetadata>)
    expectEmptyXmlByDefault
    val docRoot = new SolrDocumentGenerator(fedora, "test-pid:123").toXml
    val fields = (docRoot \ "field").map(f => (f \ "@name").text -> f.text)

    fields should contain("dc_title" -> "title")
    fields should contain("dc_description" -> "description")
    fields should contain("dc_subject" -> "subject")
    fields should contain("dc_type" -> "Dataset")
    fields should contain("dc_format" -> "format")
    fields should contain("dc_identifier" -> "easy-dataset:123")
    fields should contain("dc_source" -> "source")
    fields should contain("dc_language" -> "dut/nld")
    fields should contain("dc_publisher" -> "publisher")
    fields should contain("dc_rights" -> "OPEN_ACCESS")
    fields should contain("dc_coverage" -> "spatial")
    fields should contain("dc_date" -> "2016-01-01")
    fields should contain("dc_relation" -> "relation")
    fields should contain("dc_creator" -> "creator-org")
    fields should contain("dc_contributor" -> "contributor-org")
    fields should contain("dc_title_s" -> "title")
    fields should contain("dc_creator_s" -> "creator-org")
    fields should contain("dc_publisher_s" -> "publisher")
    fields should contain("dc_contributor_s" -> "contributor-org")
  }

  "dc_*_s sort fields with multiple values" should "contain concatenated values" in {
    expectEmd(
      <easymetadata xmlns:eas="http://easy.dans.knaw.nl/easy/easymetadata/eas/">
        <emd:title>
          <dc:title>title1</dc:title>
          <dc:title>title2</dc:title>
        </emd:title>
        <emd:publisher>
          <dc:publisher>publisher1</dc:publisher>
          <dc:publisher>publisher2</dc:publisher>
        </emd:publisher>
        <emd:creator>
          <eas:creator>
            <eas:organization>creator-org1</eas:organization>
          </eas:creator>
          <eas:creator>
            <eas:organization>creator-org2</eas:organization>
          </eas:creator>
        </emd:creator>
        <emd:contributor>
          <eas:contributor>
            <eas:organization>contributor-org1</eas:organization>
          </eas:contributor>
          <eas:contributor>
            <eas:organization>contributor-org2</eas:organization>
          </eas:contributor>
        </emd:contributor>
      </easymetadata>)
    expectEmptyXmlByDefault
    val docRoot = new SolrDocumentGenerator(fedora, "test-pid:123").toXml

    val sortTitle = getSolrDocFieldValues(docRoot, "dc_title_s")
    sortTitle should have length 1
    sortTitle should contain("title1 title2")

    val sortPublisher = getSolrDocFieldValues(docRoot, "dc_publisher_s")
    sortPublisher should have length 1
    sortPublisher should contain("publisher1 publisher2")

    val sortCreator = getSolrDocFieldValues(docRoot, "dc_creator_s")
    sortCreator should have length 1
    sortCreator should contain("creator-org1 creator-org2")

    val sortContributor = getSolrDocFieldValues(docRoot, "dc_contributor_s")
    sortContributor should have length 1
    sortContributor should contain("contributor-org1 contributor-org2")
  }

  "dc_publisher_s" should "be empty if there are no publishers" in {
    expectEmd(
      <easymetadata xmlns:eas="http://easy.dans.knaw.nl/easy/easymetadata/eas/">
        <emd:publisher>
        </emd:publisher>
      </easymetadata>)
    expectEmptyXmlByDefault
    val docRoot = new SolrDocumentGenerator(fedora, "test-pid:123").toXml

    val sortPublisher = getSolrDocFieldValues(docRoot, "dc_publisher_s")
    sortPublisher should have length 0
  }

  "dc_date" should "have formated dates for eas:scheme=\"W3CDTF\"" in {
    expectEmd(
      <easymetadata xmlns:eas="http://easy.dans.knaw.nl/easy/easymetadata/eas/">
        <emd:date>
          <dct:created>1991-01-01 to 1993-12-31</dct:created>
          <eas:dateSubmitted eas:scheme="W3CDTF" eas:format="DAY">1994-01-01T00:00:00.000+01:00</eas:dateSubmitted>
        </emd:date>
      </easymetadata>)
    expectEmptyXmlByDefault
    val docRoot = new SolrDocumentGenerator(fedora, "test-pid:123").toXml
    val dcDates = getSolrDocFieldValues(docRoot, "dc_date")
    dcDates should contain("1991-01-01 to 1993-12-31")
    dcDates should contain("1994-01-01")
  }

  "dc_creator" should "contain correctly formatted personal and organisation content" in {
    expectEmd(
      <easymetadata xmlns:eas="http://easy.dans.knaw.nl/easy/easymetadata/eas/">
        <emd:creator>
          <eas:creator>
            <eas:title>title</eas:title>
            <eas:initials>I.N.I.T.I.A.L.S.</eas:initials>
            <eas:prefix>prefix</eas:prefix>
            <eas:surname>surmane</eas:surname>
            <eas:organization>org</eas:organization>
            <eas:entityId eas:scheme="DAI"></eas:entityId>
          </eas:creator>
          <eas:creator>
            <eas:organization>org</eas:organization>
          </eas:creator>
          <dc:creator>creator-in-plain-text</dc:creator>
        </emd:creator>
      </easymetadata>)
    expectEmptyXmlByDefault
    val docRoot = new SolrDocumentGenerator(fedora, "test-pid:123").toXml
    val dcCreators = getSolrDocFieldValues(docRoot, "dc_creator")
    dcCreators should contain("surmane, title I.N.I.T.I.A.L.S. prefix (org)")
    dcCreators should contain("org")
    dcCreators should contain("creator-in-plain-text")
  }

  "dc_coverage" should "contain point and box coordinates when available" in {
  expectEmd(
    <easymetadata xmlns:eas="http://easy.dans.knaw.nl/easy/easymetadata/eas/">
      <emd:coverage>
        <dct:spatial>spatial1</dct:spatial>
        <dct:temporal eas:scheme="ABR" eas:schemeId="archaeology.dcterms.temporal">IJZL</dct:temporal>
        <eas:spatial>
          <eas:point eas:scheme="RD">
            <eas:x>155000</eas:x>
            <eas:y>463000</eas:y>
          </eas:point>
        </eas:spatial>
        <eas:spatial>
          <eas:box eas:scheme="RD">
            <eas:north>1</eas:north>
            <eas:east>3</eas:east>
            <eas:south>4</eas:south>
            <eas:west>2</eas:west>
          </eas:box>
        </eas:spatial>
      </emd:coverage>
    </easymetadata>)
    expectEmptyXmlByDefault
    val docRoot = new SolrDocumentGenerator(fedora, "test-pid:123").toXml
    val dcCoverage = getSolrDocFieldValues(docRoot, "dc_coverage")
    dcCoverage should contain("spatial1")
    dcCoverage should contain("IJZL")
    dcCoverage should contain("scheme=RD x=155000 y=463000")
    dcCoverage should contain("scheme=RD north=1 east=3 south=4 west=2")
  }

  "dc_relation" should "contain correctly handled title and uri" in {
    expectEmd(
      <easymetadata xmlns:eas="http://easy.dans.knaw.nl/easy/easymetadata/eas/">
        <emd:relation>
          <dc:relation>relation</dc:relation>
          <eas:relation>
            <eas:subject-title>no-qualif</eas:subject-title>
          </eas:relation>
          <eas:references>
            <eas:subject-title>ref-title</eas:subject-title>
            <eas:subject-link>http://dans.knaw.nl</eas:subject-link>
          </eas:references>
        </emd:relation>
      </easymetadata>)
    expectEmptyXmlByDefault
    val docRoot = new SolrDocumentGenerator(fedora, "test-pid:123").toXml
    val dcRelation = getSolrDocFieldValues(docRoot, "dc_relation")
    dcRelation should contain("relation")
    dcRelation should contain("title=no-qualif")
    dcRelation should contain("title=ref-title URI=http://dans.knaw.nl")
  }

  "archaeology_dc_subject" should "only get values from subject fields with corresponding schemeId attribute" in {
    expectEmd(
      <easymetadata
         xmlns:eas="http://easy.dans.knaw.nl/easy/easymetadata/eas/">
         <emd:subject>
           <dc:subject eas:scheme="ABR" eas:schemeId="archaeology.dc.subject">ELCF</dc:subject>
           <dc:subject>some other subject</dc:subject>
         </emd:subject>
      </easymetadata>)
      expectEmptyXmlByDefault
    val docRoot = new SolrDocumentGenerator(fedora, "test-pid:123").toXml
    val archSubjects= (docRoot \\ "field").filter(f => (f \ "@name").text == "archaeology_dc_subject").map(_.text)

    archSubjects should contain("ELCF")
    archSubjects shouldNot contain("some other subject")
  }

  "archaeology_dcterms_temporal" should "only get values from subject fields with corresponding schemeId attribute" in {
    expectEmd(
      <easymetadata
      xmlns:eas="http://easy.dans.knaw.nl/easy/easymetadata/eas/">
        <emd:coverage>
          <dcterms:temporal>some other temporal</dcterms:temporal>
          <dcterms:temporal eas:scheme="ABR" eas:schemeId="archaeology.dcterms.temporal">MESO</dcterms:temporal>
        </emd:coverage>
      </easymetadata>)
    expectEmptyXmlByDefault
    val docRoot = new SolrDocumentGenerator(fedora, "test-pid:123").toXml
    val archTemporals = getSolrDocFieldValues(docRoot, "archaeology_dcterms_temporal")

    archTemporals should contain("MESO")
    archTemporals shouldNot contain("some other temporal")
  }

  "dai_creator" should "get value from entityId field with scheme DAI in eas:creator" in {
      val CREATOR_DAI = "123456789"
      expectEmd(
        <easymetadata
        xmlns:eas="http://easy.dans.knaw.nl/easy/easymetadata/eas/">
          <emd:creator>
            <eas:creator>
              <eas:entityId scheme="DAI">{CREATOR_DAI}</eas:entityId>
            </eas:creator>
          </emd:creator>
        </easymetadata>)
      expectEmptyXmlByDefault
      val docRoot = new SolrDocumentGenerator(fedora, "test-pid:123").toXml
      val daiCreators = getSolrDocFieldValues(docRoot, "dai_creator")

      daiCreators should contain(CREATOR_DAI)
  }

  "dai_contributor" should "get value from entityId field with scheme DAI in eas:contributor" in {
    val CONTRIBUTOR_DAI = "123456789"
    expectEmd(
      <easymetadata
      xmlns:eas="http://easy.dans.knaw.nl/easy/easymetadata/eas/">
        <emd:contributor>
          <eas:contributor>
            <eas:entityId scheme="DAI">{CONTRIBUTOR_DAI}</eas:entityId>
          </eas:contributor>
        </emd:contributor>
      </easymetadata>)
    expectEmptyXmlByDefault
    val docRoot = new SolrDocumentGenerator(fedora, "test-pid:123").toXml
    val daiContributors = getSolrDocFieldValues(docRoot, "dai_contributor")

    daiContributors should contain(CONTRIBUTOR_DAI)
  }

  "mix of dai_creator and dai_contributor" should "not get mixed up" in {
    val CREATOR1_DAI = "123456789"
    val CREATOR2_DAI = "234567891"
    val CONTRIBUTOR1_DAI = "4567890123"
    val CONTRIBUTOR2_DAI = "6789012345"
    expectEmd(
      <easymetadata
      xmlns:eas="http://easy.dans.knaw.nl/easy/easymetadata/eas/">
        <emd:creator>
          <eas:creator>
            <eas:entityId scheme="DAI">{CREATOR1_DAI}</eas:entityId>
          </eas:creator>
        </emd:creator>
        <emd:creator>
          <eas:creator>
            <eas:entityId scheme="DAI">{CREATOR2_DAI}</eas:entityId>
          </eas:creator>
        </emd:creator>
        <emd:contributor>
          <eas:contributor>
            <eas:entityId scheme="DAI">{CONTRIBUTOR1_DAI}</eas:entityId>
          </eas:contributor>
          <eas:contributor>
            <eas:entityId scheme="DAI">{CONTRIBUTOR2_DAI}</eas:entityId>
          </eas:contributor>
        </emd:contributor>
      </easymetadata>)
    expectEmptyXmlByDefault
    val docRoot = new SolrDocumentGenerator(fedora, "test-pid:123").toXml
    val daiCreators = getSolrDocFieldValues(docRoot, "dai_creator")
    val daiContributors = getSolrDocFieldValues(docRoot, "dai_contributor")

    daiCreators should have length 2
    daiContributors should have length 2
    daiCreators should contain(CREATOR1_DAI)
    daiCreators should contain(CREATOR2_DAI)
    daiContributors should contain(CONTRIBUTOR1_DAI)
    daiContributors should contain(CONTRIBUTOR2_DAI)
  }

  "permission request status" should "take values from multiple fields and format it as a string" in {
    val REQUESTER_ID = "R E Quester"
    val STATE = "Granted"
    val LAST_MODIFIED = "2015-05-21T12:02:10.108+02:00"

    expectPrsl(
      <psl:permissionSequenceList xmlns:psl="http://easy.dans.knaw.nl/easy/permission-sequence-list/">
        <sequences>
          <sequence>
            <requesterId>{REQUESTER_ID}</requesterId>
            <state>{STATE}</state>
            <stateLastModified>{LAST_MODIFIED}</stateLastModified>
            <request>
              <lastRequestDate>2015-05-21T11:48:58.699+02:00</lastRequestDate>
              <acceptConditionsOfUse>true</acceptConditionsOfUse>
              <requestTitle>.</requestTitle>
              <requestTheme>.</requestTheme>
            </request>
            <reply>
              <lastReplyDate>2015-05-21T12:02:10.108+02:00</lastReplyDate>
              <replyText>.</replyText>
            </reply>
            <back-log></back-log>
          </sequence>
        </sequences>
      </psl:permissionSequenceList>)
    expectEmptyXmlByDefault
    val docRoot = new SolrDocumentGenerator(fedora, "test-pid:123").toXml
    val requestStatuses = getSolrDocFieldValues(docRoot, "psl_permission_status")

    requestStatuses should have length 1
    requestStatuses should contain(s"$REQUESTER_ID $STATE $LAST_MODIFIED")
  }

  "multiple permission request status" should "each be formatted in its own string" in {
    val REQUESTER_ID1 = "R E Quester"
    val STATE1 = "Granted"
    val LAST_MODIFIED1 = "2015-05-21T12:02:10.108+02:00"

    val REQUESTER_ID2 = "N Agger"
    val STATE2 = "Denied"
    val LAST_MODIFIED2 = "2014-05-21T12:02:10.108+02:00"

    val REQUESTER_ID3 = "N Ew"
    val STATE3 = "Submitted"
    val LAST_MODIFIED3 = "2013-05-21T12:02:10.108+02:00"

    expectPrsl(
      <psl:permissionSequenceList xmlns:psl="http://easy.dans.knaw.nl/easy/permission-sequence-list/">
        <sequences>
          <sequence>
            <requesterId>{REQUESTER_ID1}</requesterId>
            <state>{STATE1}</state>
            <stateLastModified>{LAST_MODIFIED1}</stateLastModified>
            <request>
              <lastRequestDate>2015-05-21T11:48:58.699+02:00</lastRequestDate>
              <acceptConditionsOfUse>true</acceptConditionsOfUse>
              <requestTitle>.</requestTitle>
              <requestTheme>.</requestTheme>
            </request>
            <reply>
              <lastReplyDate>2015-05-21T12:02:10.108+02:00</lastReplyDate>
              <replyText>.</replyText>
            </reply>
            <back-log></back-log>
          </sequence>
          <sequence>
            <requesterId>{REQUESTER_ID2}</requesterId>
            <state>{STATE2}</state>
            <stateLastModified>{LAST_MODIFIED2}</stateLastModified>
            <request>
              <lastRequestDate>2015-05-21T11:48:58.699+02:00</lastRequestDate>
              <acceptConditionsOfUse>true</acceptConditionsOfUse>
              <requestTitle>.</requestTitle>
              <requestTheme>.</requestTheme>
            </request>
            <reply>
              <lastReplyDate>2015-05-21T12:02:10.108+02:00</lastReplyDate>
              <replyText>.</replyText>
            </reply>
            <back-log></back-log>
          </sequence>
          <sequence>
            <requesterId>{REQUESTER_ID3}</requesterId>
            <state>{STATE3}</state>
            <stateLastModified>{LAST_MODIFIED3}</stateLastModified>
            <request>
              <lastRequestDate>2015-05-21T11:48:58.699+02:00</lastRequestDate>
              <acceptConditionsOfUse>true</acceptConditionsOfUse>
              <requestTitle>.</requestTitle>
              <requestTheme>.</requestTheme>
            </request>
            <reply>
              <lastReplyDate>2015-05-21T12:02:10.108+02:00</lastReplyDate>
              <replyText>.</replyText>
            </reply>
            <back-log></back-log>
          </sequence>
        </sequences>
      </psl:permissionSequenceList>)
    expectEmptyXmlByDefault
    val docRoot = new SolrDocumentGenerator(fedora, "test-pid:123").toXml
    val requestStatuses = getSolrDocFieldValues(docRoot, "psl_permission_status")

    requestStatuses should have length 3
    requestStatuses should contain(s"$REQUESTER_ID3 $STATE3 $LAST_MODIFIED3")
    requestStatuses should contain(s"$REQUESTER_ID2 $STATE2 $LAST_MODIFIED2")
    requestStatuses should contain(s"$REQUESTER_ID1 $STATE1 $LAST_MODIFIED1")
  }

  "easy-collection" should "be read from RELS-EXT" in {
    val COLLECTION = "info:fedora/easy-collection:5"
    val STRIPPED_COLLECTION = "easy-collection:5"

    expectRelsExt(
      <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
      <rdf:Description rdf:about="info:fedora/easy-dataset:5058">
        <hasModel xmlns="info:fedora/fedora-system:def/model#" rdf:resource="info:fedora/easy-model:EDM1DATASET"></hasModel>
        <hasModel xmlns="info:fedora/fedora-system:def/model#" rdf:resource="info:fedora/easy-model:oai-item1"></hasModel>
        <hasDoi xmlns="http://dans.knaw.nl/ontologies/relations#" rdf:parseType="Literal">10.5072/dans-zyf-v9sc</hasDoi>
        <isMemberOfOAISet xmlns="http://dans.knaw.nl/ontologies/relations#" rdf:resource="info:fedora/easy-data:oai-driverset1"></isMemberOfOAISet>
        <isCollectionMember xmlns="http://dans.knaw.nl/ontologies/relations#" rdf:resource={COLLECTION}></isCollectionMember>
        <isMemberOfOAISet xmlns="http://dans.knaw.nl/ontologies/relations#" rdf:resource="info:fedora/easy-discipline:14"></isMemberOfOAISet>
        <itemID xmlns="http://www.openarchives.org/OAI/2.0/" rdf:parseType="Literal">oai:easy.dans.knaw.nl:easy-dataset:5058</itemID>
        <hasPid xmlns="http://dans.knaw.nl/ontologies/relations#" rdf:parseType="Literal">urn:nbn:nl:ui:13-o7dy-s5</hasPid>
        <hasModel xmlns="info:fedora/fedora-system:def/model#" rdf:resource="info:fedora/dans-model:recursive-item-v1"></hasModel>
        <isMemberOf xmlns="http://dans.knaw.nl/ontologies/relations#" rdf:resource="info:fedora/easy-discipline:14"></isMemberOf>
      </rdf:Description>
    </rdf:RDF>)
    expectEmptyXmlByDefault
    val docRoot = new SolrDocumentGenerator(fedora, "test-pid:123").toXml
    val collections = getSolrDocFieldValues(docRoot, "easy_collections")

    collections should have length 1
    collections should contain(STRIPPED_COLLECTION)
  }

  "two created dates" should "result in warning logged" in {
    expectEmd(
      <easymetadata
        xmlns:eas="http://easy.dans.knaw.nl/easy/easymetadata/eas/">
        <emd:date>
          <eas:created eas:scheme="W3CDTF" eas:format="YEAR">2013-01-01T00:00:00.000+01:00</eas:created>
          <eas:created eas:scheme="W3CDTF" eas:format="YEAR">2013-01-02T00:00:00.000+01:00</eas:created>
        </emd:date>
      </easymetadata>)
    expectEmptyXmlByDefault
    expectWarningLogged()
    new SolrDocumentGenerator(fedora, "test-pid:123", log).toXml
  }

}

