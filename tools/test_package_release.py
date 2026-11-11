import unittest
from package_release import verify_certificate, verify_mapping, release_outputs, verify_abi, verify_release_notes
from pathlib import Path
from tempfile import TemporaryDirectory
from zipfile import ZipFile


class CertificateTest(unittest.TestCase):
    def test_packaged_notes_must_match_shared_source(self):
        with TemporaryDirectory() as folder:
            apk = Path(folder) / 'fixture.apk'
            with ZipFile(apk, 'w') as archive:
                archive.writestr('assets/releases.json', b'{"releases":[]}')
            verify_release_notes(apk, b'{"releases":[]}')
            with self.assertRaises(ValueError): verify_release_notes(apk, b'other notes')
            with ZipFile(apk, 'w') as archive:
                archive.writestr('other.json', b'{}')
            with self.assertRaises(ValueError): verify_release_notes(apk, b'{}')

    def test_release_outputs_reject_missing_duplicate_wrong_version_and_unsafe_paths(self):
        abis = ('arm64-v8a', 'armeabi-v7a', 'x86', 'x86_64')
        def element(abi):
            return {'filters': [] if abi == 'universal' else [{'filterType': 'ABI', 'value': abi}],
                    'versionName': '1.2.2', 'versionCode': 22, 'outputFile': f'app-{abi}-release.apk'}
        good = {'applicationId': 'com.kmab.singalong', 'elements': [element(a) for a in ('universal', *abis)]}
        self.assertEqual(set(('universal', *abis)), set(release_outputs(good, '1.2.2', 22)))
        for bad in (
            {**good, 'elements': good['elements'][:-1]},
            {**good, 'elements': [*good['elements'], element('x86')]},
            {**good, 'elements': [{**element('universal'), 'outputFile': '../private.apk'}, *good['elements'][1:]]},
            {**good, 'elements': [{**element('universal'), 'versionCode': 21}, *good['elements'][1:]]},
            {**good, 'applicationId': 'wrong.package'},
        ):
            with self.assertRaises(ValueError): release_outputs(bad, '1.2.2', 22)

    def test_apk_native_architecture_must_match_its_download_label(self):
        with TemporaryDirectory() as folder:
            apk = Path(folder) / 'fixture.apk'
            with ZipFile(apk, 'w') as archive:
                archive.writestr('lib/arm64-v8a/libsample.so', b'native')
            verify_abi(apk, 'arm64-v8a')
            with self.assertRaises(ValueError): verify_abi(apk, 'x86')
            with self.assertRaises(ValueError): verify_abi(apk, 'universal')
            with ZipFile(apk, 'w') as archive:
                for abi in ('arm64-v8a', 'armeabi-v7a', 'x86', 'x86_64'):
                    archive.writestr(f'lib/{abi}/libsample.so', b'native')
            verify_abi(apk, 'universal')

    def test_mapping_from_another_build_is_rejected(self):
        with TemporaryDirectory() as folder:
            apk, mapping = Path(folder) / 'fixture.apk', Path(folder) / 'mapping.txt'
            with ZipFile(apk, 'w') as archive:
                archive.writestr('classes.dex', b'source r8-map-id-abc123 end')
            mapping.write_text('# pg_map_id: abc123\n')
            self.assertEqual('abc123', verify_mapping(apk, mapping))
            mapping.write_text('# pg_map_id: def456\n')
            with self.assertRaises(ValueError): verify_mapping(apk, mapping)

    def test_only_the_expected_single_signer_is_accepted(self):
        expected = 'ab' * 32
        good = f'Verifies\nNumber of signers: 1\nSigner #1 certificate SHA-256 digest: {expected.upper()}\n'
        verify_certificate(good, expected)
        for bad in (good.replace(expected.upper(), 'cd' * 32),
                    good.replace('signers: 1', 'signers: 2'),
                    good.replace('signers: 1', 'signers: 10'),
                    'Verifies\n'):
            with self.assertRaises(ValueError): verify_certificate(bad, expected)


if __name__ == '__main__': unittest.main()
