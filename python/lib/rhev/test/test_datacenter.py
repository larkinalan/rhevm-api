#
# This file is part of python-rhev. python-rhev is free software that is
# made available under the MIT license. Consult the file "LICENSE" that
# is distributed together with this file for the exact licensing terms.
#
# python-rhev is copyright (c) 2010 by the python-rhev authors. See the
# file "AUTHORS" for a complete overview.

from rhev import *
from rhev.test import util
from rhev.test.base import BaseTest
from rhev.test.loader import depends
from nose.tools import assert_raises


class TestDataCenter(BaseTest):

    def _test_use_existing(self, dc):
        dc = self.api.get(schema.DataCenter, name=dc)
        assert dc is not None
        self.store.dc = dc

    def _test_create(self):
        dc = schema.new(schema.DataCenter)
        dc.name = util.random_name('dc')
        dc.storage_type = 'NFS'
        dc.version = self.get_version()
        dc2 = self.api.create(dc)
        assert dc2.id is not None
        assert dc2.href is not None
        assert dc2.storage_type == 'NFS'
        self.store.dc = dc2

    @depends(_test_use_existing, _test_create)
    def _test_get(self):
        dc = self.store.dc
        dc2 = self.api.get(schema.DataCenter, id=dc.id)
        assert dc2 is not None
        assert isinstance(dc2, schema.DataCenter)
        assert dc2.id == dc.id

    @depends(_test_use_existing, _test_create)
    def _test_reload(self):
        dc = self.store.dc
        dc2 = self.api.reload(dc)
        assert dc2 is not None
        assert isinstance(dc2, schema.DataCenter)
        assert dc2.id == dc.id

    @depends(_test_use_existing, _test_create)
    def _test_getall(self):
        dc = self.store.dc
        dcs = self.api.getall(schema.DataCenter)
        assert isinstance(dcs, schema.DataCenters)
        assert len(dcs) > 0
        assert util.contains_id(dcs, dc.id)

    @depends(_test_use_existing, _test_create)
    def _test_search(self):
        dc = self.store.dc
        dcs = self.api.getall(schema.DataCenter, name=dc.name)
        assert isinstance(dcs, schema.DataCenters)
        assert len(dcs) == 1
        assert dcs[0].id == dc.id
        dcs = self.api.getall(schema.DataCenter, search='name=%s' % dc.name)
        assert isinstance(dcs, schema.DataCenters)
        assert len(dcs) == 1
        assert dcs[0].id == dc.id

    @depends(_test_use_existing, _test_create)
    def _test_attributes(self):
        dc = self.store.dc
        assert util.is_str_uuid(dc.id)
        assert util.is_str(dc.href) and dc.href.endswith(dc.id)
        assert util.is_str(dc.name) and len(dc.name) > 0
        assert dc.description is None or util.is_str(dc.description)
        assert dc.storage_type in ('NFS', 'ISCSI', 'FCP')
        # A few statuses are probably missing
        assert dc.status in ('UP', 'DOWN', 'ERROR', 'UNINITIALIZED',
                'MAINTENANCE', 'NON_OPERATIONAL', 'NON_RESPONSIVE',
                'PROBLEMATIC')
        assert dc.version is not None
        assert util.is_int(dc.version.major) and dc.version.major > 0
        assert util.is_int(dc.version.minor)
        assert dc.supported_versions is not None
        for version in dc.supported_versions.version:
            assert util.is_int(version.major) and version.major > 0
            assert util.is_int(version.minor)

    @depends(_test_use_existing, _test_create)
    def _test_update(self):
        dc = self.store.dc
        dc.description = 'foobar'
        dc2 = self.api.update(dc)
        assert dc2.description == 'foobar'
        dc = self.api.get(schema.DataCenter, id=dc2.id)
        assert dc.description == 'foobar'

    @depends(_test_use_existing, _test_create)
    def _test_delete(self):
        dc = self.store.dc
        dc2 = self.api.delete(dc)
        assert dc2 is None
        dc2 = self.api.get(schema.DataCenter, name=dc.name)
        assert dc2 is None

    @depends(_test_use_existing, _test_create)
    def _test_has_storagedomains(self):
        dc = self.store.dc
        sds = self.api.getall(schema.StorageDomain, base=dc)
        assert isinstance(sds, schema.StorageDomains)
        for sd in sds:
            assert isinstance(sd, schema.StorageDomain)
            assert util.is_str_uuid(sd.id)

    @depends(_test_use_existing, _test_create)
    def _test_has_files(self):
        dc = self.store.dc
        files = self.api.getall(schema.Files, base=dc)
        assert isinstance(files, schema.Files)
        for file in files:
            assert isinstance(file, schema.File)

    @depends(_test_has_files)
    def _test_file_attributes(self):
        dc = self.store.dc
        files = self.api.getall(schema.Files, base=dc)
        for file in files:
            assert util.is_str(file.id) and len(file.id) > 0
            assert util.is_str_href(file.href) and file.href.endswith(file.id)
            assert util.is_str(file.name)
            assert file.type in ('ISO', 'VFD')
            assert isinstance(file.data_center, schema.DataCenter)
            assert file.data_center.id == dc.id

    def test_datacenter(self):
        dcs = self.api.getall(schema.DataCenter)
        dcs = [ dc.name for dc in dcs ]
        # non-intrusive tests on existing datacenters
        for dc in dcs:
            yield self._test_use_existing, dc
            yield self._test_get
            yield self._test_reload
            yield self._test_getall
            yield self._test_search
            yield self._test_attributes
            yield self._test_has_storagedomains
            yield self._test_has_files
            yield self._test_file_attributes
        # intrusive tests on a newly created datacenter
        yield self._test_create
        yield self._test_get
        yield self._test_reload
        yield self._test_getall
        yield self._test_search
        yield self._test_attributes
        yield self._test_update
        yield self._test_delete

    def test_prepare_nonexistent(self):
        dc = schema.new(schema.DataCenter)
        dc.id = 'foo'
        dc.href = '%s/datacenters/foo' % self.api.entrypoint
        self.store.dc = dc

    @depends(test_prepare_nonexistent)
    def test_get_nonexistent(self):
        dc = self.store.dc
        dc = self.api.get(schema.DataCenter, id=dc.id)
        assert dc is None

    @depends(test_prepare_nonexistent)
    def test_reload_nonexistent(self):
        dc = self.store.dc
        dc = self.api.reload(dc)
        assert dc is None

    @depends(test_prepare_nonexistent)
    def test_update_nonexistent(self):
        dc = self.store.dc
        assert_raises(NotFound, self.api.update, dc)

    @depends(test_prepare_nonexistent)
    def test_delete_nonexistent(self):
        dc = self.store.dc
        assert_raises(NotFound, self.api.delete, dc)
