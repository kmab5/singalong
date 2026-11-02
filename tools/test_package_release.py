import unittest
from package_release import verify_certificate, verify_mapping
from pathlib import Path
from tempfile import TemporaryDirectory
from zipfile import ZipFile


class CertificateTest(unittest.TestCase):
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
